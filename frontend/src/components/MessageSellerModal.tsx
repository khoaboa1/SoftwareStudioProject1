import { useAuth } from '../lib/auth-context'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import type { Listing } from '../services/listingService'
import {
  getConversations,
  getMessages,
  sendMessage,
  startConversation,
  type Message,
} from '../services/conversationService'
import { FormAlert } from './ui/FormAlert'
import { MessageBubble } from './MessageBubble'
import { MessageComposer } from './MessageComposer'
import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'

export type MessageSellerModalProps = {
  listing: Listing
  onClose: () => void
}

/**
 * A modal chat popup for a listing.
 *
 * The conversation is only created when the first message is sent. Until then
 * the modal shows an empty prompt; existing history is loaded on mount when a
 * conversation already exists for this buyer and listing.
 */
export function MessageSellerModal({
  listing,
  onClose,
}: MessageSellerModalProps) {
  const { student } = useAuth()
  const navigate = useNavigate()
  const panelRef = useRef<HTMLDivElement>(null)
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [composerText, setComposerText] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    panelRef.current?.focus()
  }, [])

  useEffect(() => {
    function onKeydown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKeydown)
    return () => {
      document.removeEventListener('keydown', onKeydown)
    }
  }, [onClose])

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const conversations = await getConversations()
        if (cancelled) return
        const existing = conversations.find(
          (c) => c.listingId === listing.id && c.buyerId === student?.id
        )
        if (existing) {
          setConversationId(existing.id)
          const history = await getMessages(existing.id)
          if (!cancelled) {
            setMessages(history)
          }
        }
      } catch (error) {
        if (cancelled) return
        if (hasApiStatus(error, 401)) {
          navigate('/login')
        } else {
          setLoadError(getErrorMessage(error, 'Could not load messages.'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [listing.id, student?.id, navigate])

  async function handleSend(content: string) {
    setSendError(null)
    setSending(true)
    setComposerText('')
    try {
      const id = conversationId ?? (await startConversation(listing.id)).id
      setConversationId(id)
      const saved = await sendMessage(id, content)
      setMessages((prev) => [...prev, saved])
      setLoadError(null)
    } catch (error) {
      if (hasApiStatus(error, 401)) {
        navigate('/login')
      } else {
        setSendError(getErrorMessage(error, 'Could not send message.'))
        setComposerText(content)
      }
    } finally {
      setSending(false)
    }
  }

  return (
    <div
      data-testid="message-modal-backdrop"
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/40 p-4"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose()
        }
      }}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="message-seller-title"
        tabIndex={-1}
        className="flex max-h-[90dvh] w-full max-w-lg flex-col rounded-2xl bg-white p-5 shadow-xl"
      >
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2
              id="message-seller-title"
              className="text-lg font-semibold text-zinc-900"
            >
              {listing.itemName}
            </h2>
            <p className="text-sm text-zinc-500">
              Sold by {listing.sellerName} · ${listing.price.toFixed(2)}
            </p>
          </div>
          <button
            type="button"
            aria-label="Close"
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-900"
          >
            ×
          </button>
        </div>
        {conversationId && (
          <Link
            to={`/conversations/${conversationId}`}
            className="text-sm font-medium text-accent-700 hover:text-accent-800"
          >
            Open full conversation
          </Link>
        )}
        {loading ? (
          <p role="status" className="text-sm text-zinc-500">
            Loading messages…
          </p>
        ) : loadError ? (
          <FormAlert kind="error">{loadError}</FormAlert>
        ) : messages.length === 0 ? (
          <p className="text-sm text-zinc-600">
            No messages yet. Say hi about {listing.itemName}.
          </p>
        ) : (
          <ul
            aria-label="Messages"
            className="my-4 flex-1 space-y-3 overflow-y-auto"
          >
            {messages.map((m) => (
              <MessageBubble
                key={m.id}
                content={m.content}
                createdAt={m.createdAt}
                mine={m.senderId === student?.id}
              />
            ))}
          </ul>
        )}
        {sendError && <FormAlert kind="error">{sendError}</FormAlert>}
        <MessageComposer
          value={composerText}
          onChange={setComposerText}
          onSend={handleSend}
          sending={sending}
        />
      </div>
    </div>
  )
}
