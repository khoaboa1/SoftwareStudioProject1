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

  const messagesEndRef = useRef<HTMLDivElement>(null)

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
        if (hasApiStatus(error, 404) && error instanceof ApiError && error.message === 'Profile not found') {
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

  /* Initial message load */
  useEffect(() => {
    let cancelled = false
    getMessages(conversationId)
      .then((data) => {
        if (!cancelled) {
          setMessages(data.map((m) => ({ ...m })))
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
  }, [conversationId])

  /* Poll every 5 seconds, pause when tab hidden, stop on unmount */
  useEffect(() => {
    if (!messagesLoaded) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'hidden') return
      const after = messages.length > 0 ? messages[messages.length - 1].id : undefined
      getMessages(conversationId, after)
        .then((data) => {
          const existingIds = new Set(messages.map((m) => m.id))
          const uniqueNew = data.filter((m) => !existingIds.has(m.id))
          if (uniqueNew.length > 0) {
            setMessages((prev) => [
              ...prev,
              ...uniqueNew.map((m) => ({ ...m })),
            ])
          }
        })
    }, POLL_MS)
    return () => clearInterval(timer)
  }, [conversationId, messagesLoaded, messages])

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
          setMessages((prev) =>
            prev.map((m) => (m.id === tempId ? { ...saved } : m)),
          )
          setSendingTempId(null)
        })
        .catch((error) => {
          if (error instanceof ApiError && error.status === 400) {
            setSendError(error.message)
            setMessages((prev) => prev.filter((m) => m.id !== tempId))
          } else {
            setMessages((prev) =>
              prev.map((m) => (m.id === tempId ? { ...m, status: 'failed' } : m)),
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
          setMessages((prev) =>
            prev.map((m) => (m.id === tempId ? { ...saved } : m)),
          )
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

  /* Scroll to newest message whenever the list changes */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView()
  }, [messages])

  /* ── Conditional render (after all hooks) ── */

  /* Not a numeric id → conversation not found */
  if (!validId) {
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

  /* Conversation-level errors */
  if (convState.status === 'error') {
    const err = convState.error as ApiError
    if (hasApiStatus(err, 403) || hasApiStatus(err, 404)) {
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
    return (
      <div className="min-h-dvh bg-zinc-50">
        <AppHeader />
        <main className="mx-auto max-w-6xl px-4 py-8">
          <FormAlert kind="error">
            {getErrorMessage(convState.error, 'Could not load this conversation.')}
          </FormAlert>
          <Button type="button" onClick={reloadConv} className="mt-4">
            Retry
          </Button>
        </main>
      </div>
    )
  }

  /* Loading conversation */
  if (convState.status !== 'ready') {
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

  /* Ready → render chat */
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
          ref={messagesEndRef}
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
          onSend={handleSend}
          sending={sendingTempId !== null}
        />
      </main>
    </div>
  )
}
