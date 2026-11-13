import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { MessageBubble } from '../components/MessageBubble'
import { MessageComposer } from '../components/MessageComposer'
import { useAuth } from '../lib/auth-context'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import { ApiError } from '../services/httpClient'
import {
  getConversation,
  getMessages,
  sendMessage,
  type Message,
} from '../services/conversationService'

const POLL_MS = 5000

type MessageWithStatus = Message & { status?: 'sending' | 'failed' }

/**
 * Chat page for a single conversation.
 *
 * Visual direction (Swiss anchor): left-aligned header, single coral accent for mine,
 * neutral grey for theirs, hairline structure.
 *
 * Accessibility:
 * - Heading names the other party.
 * - <ul aria-label="Messages"> for the list.
 * - Each bubble has data-mine and aria-busy while sending.
 * - Failed bubbles show "Not sent" + Retry.
 * - 401 → /login, 404 "Profile not found" → /profile-setup, 403/other 404 → "Conversation not found".
 */
export function ConversationPage() {
  const { id } = useParams()
  const conversationId = Number(id)
  const validId = Number.isInteger(conversationId) && conversationId > 0
  const { student } = useAuth()
  const navigate = useNavigate()
  const currentStudentId = student?.id

  const listRef = useRef<HTMLUListElement>(null)
  /** Highest real (positive) message id seen — used as the polling cursor. */
  const lastMessageIdRef = useRef<number | undefined>(undefined)
  /** Mirror of `messages` kept current so the poll timer never needs `messages`
     in its dependency array (and thus never restarts on every message). */
  const messagesRef = useRef<MessageWithStatus[]>([])
  /** The conversation id the current poll timer belongs to — used to drop
     stale poll results that arrive after the user switched conversations. */
  const activeIdRef = useRef(conversationId)
  useEffect(() => {
    activeIdRef.current = conversationId
  }, [conversationId])

  /* ── Hooks (always, same order) ── */
  const { state: convState, reload: reloadConv } = useAsyncResource(
    () => getConversation(conversationId),
    [conversationId],
    {
      enabled: validId,
      onError: (error) => {
        if (hasApiStatus(error, 401)) {
          navigate('/login')
          return true
        }
        if (error instanceof ApiError && error.message === 'Profile not found') {
          navigate('/profile-setup')
          return true
        }
        return false
      },
    },
  )

  const [messages, setMessages] = useState<MessageWithStatus[]>([])
  const [messagesError, setMessagesError] = useState<string | null>(null)
  const [messagesLoaded, setMessagesLoaded] = useState(false)
  const [sendingTempId, setSendingTempId] = useState<number | null>(null)
  const [sendError, setSendError] = useState<string | null>(null)
  const [composerText, setComposerText] = useState('')

  /* Keep the messages mirror current for the poll timer. */
  useEffect(() => {
    messagesRef.current = messages
  }, [messages])

  /* Reset all message state when the conversation id changes (e.g. picking
     another chat from the dropdown on the same page instance). Resetting via
     effect is intentional here: the old messages must clear immediately when
     the user picks a different conversation from the dropdown. */
  /* oxlint-disable react(set-state-in-effect) */
  useEffect(() => {
    setMessages([])
    setMessagesError(null)
    setMessagesLoaded(false)
    setSendingTempId(null)
    setSendError(null)
    setComposerText('')
    lastMessageIdRef.current = undefined
  }, [conversationId])
  /* oxlint-enable react(set-state-in-effect) */

  /* Initial message load — only for a valid numeric id. */
  useEffect(() => {
    if (!validId) return
    let cancelled = false
    getMessages(conversationId)
      .then((data) => {
        if (!cancelled) {
          setMessages(data.map((m) => ({ ...m })))
          const last = data[data.length - 1]
          if (last && last.id > 0) lastMessageIdRef.current = last.id
          setMessagesLoaded(true)
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setMessagesError(getErrorMessage(error, 'Could not load messages.'))
          setMessagesLoaded(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [conversationId, validId])

  /* Poll every 5 seconds, only after the conversation itself has loaded.
     Uses a ref for the cursor so the timer isn't torn down on every message. */
  useEffect(() => {
    if (convState.status !== 'ready' || !messagesLoaded) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'hidden') return
      const after = lastMessageIdRef.current
      getMessages(conversationId, after)
        .then((data) => {
          // Drop stale poll results that arrived after the user switched
          // conversations (e.g. from the Message dropdown).
          if (activeIdRef.current !== conversationId) return
          const existingIds = new Set(messagesRef.current.map((m) => m.id))
          const uniqueNew = data.filter((m) => !existingIds.has(m.id))
          if (uniqueNew.length > 0) {
            setMessages((prev) => {
              const next = [...prev, ...uniqueNew.map((m) => ({ ...m }))]
              const last = next[next.length - 1]
              if (last && last.id > 0) lastMessageIdRef.current = last.id
              return next
            })
          }
        })
        .catch((error) => {
          if (error instanceof ApiError && error.status === 401) {
            navigate('/login')
          }
        })
    }, POLL_MS)
    return () => clearInterval(timer)
  }, [conversationId, messagesLoaded, convState.status, navigate])

  /* Scroll to the newest message whenever the list changes. */
  useEffect(() => {
    const list = listRef.current
    if (!list) return
    const last = list.lastElementChild
    if (last) last.scrollIntoView({ block: 'end' })
  }, [messages])

  /* ── Sending ── */
  const handleSend = useCallback(
    (content: string) => {
      setSendError(null)
      const tempId = -Date.now()
      const optimistic: MessageWithStatus = {
        id: tempId,
        conversationId,
        senderId: currentStudentId!,
        senderName: student?.studentName ?? 'You',
        content,
        createdAt: new Date().toISOString(),
        status: 'sending',
      }
      setMessages((prev) => [...prev, optimistic])
      setSendingTempId(tempId)

      sendMessage(conversationId, content)
        .then((saved) => {
          setMessages((prev) => {
            // Drop the optimistic placeholder and any polling copy that
            // already arrived with this id, then add the server's copy.
            const cleaned = prev.filter(
              (m) => m.id !== tempId && m.id !== saved.id,
            )
            return [...cleaned, { ...saved }]
          })
          setSendingTempId(null)
          setComposerText('')
        })
        .catch((error) => {
          if (error instanceof ApiError && error.status === 400) {
            setSendError(error.message)
            // Keep the text in the box; drop the optimistic placeholder.
            setMessages((prev) => prev.filter((m) => m.id !== tempId))
          } else {
            setMessages((prev) =>
              prev.map((m) =>
                m.id === tempId ? { ...m, status: 'failed' } : m,
              ),
            )
          }
          setSendingTempId(null)
        })
    },
    [conversationId, currentStudentId, student?.studentName],
  )

  const handleRetry = useCallback(
    (tempId: number) => {
      const msg = messages.find((m) => m.id === tempId && m.status === 'failed')
      if (!msg) return
      setSendError(null)
      setMessages((prev) =>
        prev.map((m) => (m.id === tempId ? { ...m, status: 'sending' } : m)),
      )
      setSendingTempId(tempId)

      sendMessage(conversationId, msg.content)
        .then((saved) => {
          setMessages((prev) => {
            const cleaned = prev.filter(
              (m) => m.id !== tempId && m.id !== saved.id,
            )
            return [...cleaned, { ...saved }]
          })
          setSendingTempId(null)
        })
        .catch(() => {
          setMessages((prev) =>
            prev.map((m) => (m.id === tempId ? { ...m, status: 'failed' } : m)),
          )
          setSendingTempId(null)
        })
    },
    [messages, conversationId],
  )

  /* Reload both the conversation and its messages. */
  const reloadAll = useCallback(() => {
    reloadConv()
    setMessagesLoaded(false)
    setMessagesError(null)
    getMessages(conversationId)
      .then((data) => {
        setMessages(data.map((m) => ({ ...m })))
        const last = data[data.length - 1]
        if (last && last.id > 0) lastMessageIdRef.current = last.id
        setMessagesLoaded(true)
      })
      .catch((error) => {
        setMessagesError(getErrorMessage(error, 'Could not load messages.'))
        setMessagesLoaded(true)
      })
  }, [conversationId, reloadConv])

  /* ── Conditional render (after all hooks) ── */

  if (!validId) {
    return notFoundView()
  }

  if (convState.status === 'error') {
    const err = convState.error
    if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
      return notFoundView()
    }
    // oxlint-disable-next-line react(refs)
    return errorView(getErrorMessage(err, 'Could not load this conversation.'), reloadAll)
  }

  if (convState.status !== 'ready') {
    return loadingView()
  }

  const conversation = convState.data
  const isBuyer = conversation.buyerId === currentStudentId
  const otherPartyName = isBuyer ? conversation.sellerName : conversation.buyerName
  const sold = conversation.listingStatus === 'SOLD'

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-4 py-8">
        <div className="mb-8 flex items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">{otherPartyName}</h1>
            <p className="mt-1 text-sm text-zinc-500">
              <Link to={`/listings/${conversation.listingId}`}>{conversation.itemName}</Link>
            </p>
          </div>
          <Link to="/inbox" className="mt-2 inline-block text-sm font-medium text-accent-700 hover:text-accent-800">
            ← All messages
          </Link>
        </div>

        {sold && (
          <span className="mb-6 inline-block rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
            Sold
          </span>
        )}

        {messagesError && <FormAlert kind="error">{messagesError}</FormAlert>}

        <ul
          ref={listRef}
          aria-label="Messages"
          className="space-y-3 mb-6 max-h-[60vh] overflow-y-auto"
        >
          {messages.map((message) => (
            <MessageBubble
              key={message.id}
              content={message.content}
              createdAt={message.createdAt}
              mine={message.senderId === currentStudentId}
              status={message.status}
              onRetry={message.status === 'failed' ? () => handleRetry(message.id) : undefined}
            />
          ))}
        </ul>

        {messagesLoaded && messages.length === 0 && !sendError && (
          <div className="mb-6 rounded-2xl border border-zinc-200 bg-white p-10 text-center">
            <p className="text-sm font-medium text-zinc-900">
              No messages yet. Say hi about {conversation.itemName}.
            </p>
          </div>
        )}

        {sendError && <FormAlert kind="error">{sendError}</FormAlert>}

        <MessageComposer
          value={composerText}
          onChange={setComposerText}
          onSend={handleSend}
          sending={sendingTempId !== null}
        />
      </main>
    </div>
  )

  function notFoundView() {
    return (
      <div className="min-h-dvh bg-zinc-50">
        <AppHeader />
        <main className="mx-auto max-w-6xl px-4 py-8">
          <h1 className="text-xl font-semibold text-zinc-900">Conversation not found</h1>
          <Link
            to="/inbox"
            className="mt-2 inline-block text-sm font-medium text-accent-700 hover:text-accent-800"
          >
            ← All messages
          </Link>
        </main>
      </div>
    )
  }

  function errorView(message: string, onRetry: () => void) {
    return (
      <div className="min-h-dvh bg-zinc-50">
        <AppHeader />
        <main className="mx-auto max-w-6xl px-4 py-8">
          <FormAlert kind="error">{message}</FormAlert>
          <Button type="button" onClick={onRetry} className="mt-4">
            Retry
          </Button>
        </main>
      </div>
    )
  }

  function loadingView() {
    return (
      <div className="min-h-dvh bg-zinc-50">
        <AppHeader />
        <main className="mx-auto max-w-6xl px-4 py-8">
          <div role="status" className="py-8 text-center">
            <span className="sr-only">Loading conversation</span>
          </div>
        </main>
      </div>
    )
  }
}
