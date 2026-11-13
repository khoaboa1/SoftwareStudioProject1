import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { MessageBubble } from '../components/MessageBubble'
import { MessageComposer } from '../components/MessageComposer'
import { useAuth } from '../lib/auth-context'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import {
  getConversation,
  getMessages,
  sendMessage,
  type Message,
} from '../services/conversationService'

const POLL_MS = 5000

type MessageWithStatus = Message & { status?: 'sending' | 'failed' }

function PageShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
    </div>
  )
}

function BackToInbox() {
  return (
    <Link to="/inbox" className="mt-2 inline-block text-sm font-medium text-accent-700 hover:text-accent-800">
      ← All messages
    </Link>
  )
}

function NotFound() {
  return (
    <PageShell>
      <h1 className="text-xl font-semibold text-zinc-900">Conversation not found</h1>
      <BackToInbox />
    </PageShell>
  )
}

/** Highest real (server-assigned, positive) message id, used as the polling cursor. */
function lastRealId(messages: MessageWithStatus[]): number | undefined {
  let last: number | undefined
  for (const message of messages) {
    if (message.id > 0 && (last === undefined || message.id > last)) last = message.id
  }
  return last
}

/**
 * Chat page for a single conversation.
 *
 * The chat is keyed by conversation id, so opening another conversation (for example from the
 * Message dropdown) remounts it: old messages, timers and in-flight results are dropped with it.
 */
export function ConversationPage() {
  const { id } = useParams()
  return <ConversationChat key={id} id={id} />
}

function ConversationChat({ id }: { id: string | undefined }) {
  const conversationId = Number(id)
  const validId = Number.isInteger(conversationId) && conversationId > 0
  const { student } = useAuth()
  const navigate = useNavigate()
  const currentStudentId = student?.id ?? 0

  const listRef = useRef<HTMLUListElement>(null)
  const cursorRef = useRef<number | undefined>(undefined)

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
        if (hasApiStatus(error, 404) && error.message === 'Profile not found') {
          navigate('/profile-setup')
          return true
        }
        return false
      },
    },
  )
  const ready = convState.status === 'ready'

  const [messages, setMessages] = useState<MessageWithStatus[]>([])
  const [messagesError, setMessagesError] = useState<string | null>(null)
  const [messagesLoaded, setMessagesLoaded] = useState(false)
  const [sendingCount, setSendingCount] = useState(0)
  const [sendError, setSendError] = useState<string | null>(null)
  const [composerText, setComposerText] = useState('')

  useEffect(() => {
    cursorRef.current = lastRealId(messages)
  }, [messages])

  /* Initial history — only once the conversation itself has loaded. */
  useEffect(() => {
    if (!ready) return
    let cancelled = false
    getMessages(conversationId)
      .then((data) => {
        if (cancelled) return
        setMessages(data)
        setMessagesLoaded(true)
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (hasApiStatus(error, 401)) {
          navigate('/login')
          return
        }
        setMessagesError(getErrorMessage(error, 'Could not load messages.'))
        setMessagesLoaded(true)
      })
    return () => { cancelled = true }
  }, [conversationId, ready, navigate])

  /* Poll every 5 seconds for messages after the last real one; pause while the tab is hidden. */
  useEffect(() => {
    if (!ready || !messagesLoaded) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'hidden') return
      getMessages(conversationId, cursorRef.current)
        .then((data) => {
          setMessagesError(null)
          setMessages((prev) => {
            const known = new Set(prev.map((m) => m.id))
            const fresh = data.filter((m) => !known.has(m.id))
            return fresh.length > 0 ? [...prev, ...fresh] : prev
          })
        })
        .catch((error: unknown) => {
          if (hasApiStatus(error, 401)) navigate('/login')
        })
    }, POLL_MS)
    return () => clearInterval(timer)
  }, [conversationId, ready, messagesLoaded, navigate])

  /* Keep the newest message in view. */
  useEffect(() => {
    listRef.current?.lastElementChild?.scrollIntoView({ block: 'end' })
  }, [messages])

  /* Sends (or re-sends) the optimistic message `tempId`. */
  const deliver = useCallback(
    (tempId: number, content: string) => {
      setSendingCount((count) => count + 1)
      sendMessage(conversationId, content)
        .then((saved) => {
          // Replace the placeholder in place (keeps order) and drop any polled copy of the same message.
          setMessages((prev) =>
            prev.filter((m) => m.id !== saved.id).map((m) => (m.id === tempId ? saved : m)),
          )
        })
        .catch((error: unknown) => {
          if (hasApiStatus(error, 401)) {
            navigate('/login')
          } else if (hasApiStatus(error, 400)) {
            // Rejected content: show why and give the text back for editing.
            setSendError(error.message)
            setComposerText(content)
            setMessages((prev) => prev.filter((m) => m.id !== tempId))
          } else {
            setMessages((prev) => prev.map((m) => (m.id === tempId ? { ...m, status: 'failed' } : m)))
          }
        })
        .finally(() => setSendingCount((count) => count - 1))
    },
    [conversationId, navigate],
  )

  function handleSend(content: string) {
    setSendError(null)
    const tempId = -Date.now()
    setMessages((prev) => [
      ...prev,
      {
        id: tempId,
        conversationId,
        senderId: currentStudentId,
        senderName: student?.studentName ?? 'You',
        content,
        createdAt: new Date().toISOString(),
        status: 'sending',
      },
    ])
    deliver(tempId, content)
  }

  function handleRetry(message: MessageWithStatus) {
    setSendError(null)
    setMessages((prev) => prev.map((m) => (m.id === message.id ? { ...m, status: 'sending' } : m)))
    deliver(message.id, message.content)
  }

  if (!validId) return <NotFound />

  if (convState.status === 'error') {
    if (hasApiStatus(convState.error, 403) || hasApiStatus(convState.error, 404)) return <NotFound />
    return (
      <PageShell>
        <FormAlert kind="error">{getErrorMessage(convState.error, 'Could not load this conversation.')}</FormAlert>
        <Button type="button" onClick={reloadConv} className="mt-4">Retry</Button>
      </PageShell>
    )
  }

  if (convState.status !== 'ready') {
    return (
      <PageShell>
        <div role="status" className="py-8 text-center">
          <span className="sr-only">Loading conversation</span>
        </div>
      </PageShell>
    )
  }

  const conversation = convState.data
  const otherPartyName = conversation.buyerId === currentStudentId ? conversation.sellerName : conversation.buyerName

  return (
    <PageShell>
      <div className="mb-8 flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-zinc-900">{otherPartyName}</h1>
          <p className="mt-1 text-sm text-zinc-500">
            <Link to={`/listings/${conversation.listingId}`}>{conversation.itemName}</Link>
          </p>
        </div>
        <BackToInbox />
      </div>

      {conversation.listingStatus === 'SOLD' && (
        <span className="mb-6 inline-block rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
          Sold
        </span>
      )}

      {messagesError && <FormAlert kind="error">{messagesError}</FormAlert>}

      <ul ref={listRef} aria-label="Messages" className="mb-6 max-h-[60vh] space-y-3 overflow-y-auto">
        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            content={message.content}
            createdAt={message.createdAt}
            mine={message.senderId === currentStudentId}
            status={message.status}
            onRetry={message.status === 'failed' ? () => handleRetry(message) : undefined}
          />
        ))}
      </ul>

      {messagesLoaded && messages.length === 0 && !messagesError && !sendError && (
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
        sending={sendingCount > 0}
      />
    </PageShell>
  )
}
