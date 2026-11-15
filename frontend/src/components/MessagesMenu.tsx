import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChatCircleDots } from '@phosphor-icons/react'
import { ConversationRow } from './ConversationRow'
import { getErrorMessage } from '../lib/apiError'
import { useAuth } from '../lib/auth-context'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getConversations, sortNewestFirst, toSummary } from '../services/conversationService'

const PREVIEW_COUNT = 3

/** Header button that opens a dropdown: "All messages" plus the latest 3 conversations. */
export function MessagesMenu() {
  const { student } = useAuth()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const { state, reload } = useAsyncResource(() => getConversations(), [], { enabled: open })

  useEffect(() => {
    if (!open) return
    function onPointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const latest = state.status === 'ready' && student
    ? sortNewestFirst(state.data).slice(0, PREVIEW_COUNT).map((c) => toSummary(c, student.id))
    : []

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-accent-500"
      >
        <ChatCircleDots aria-hidden="true" className="h-5 w-5" />
        Message
      </button>

      {open && (
        <div
          role="region"
          aria-label="Messages"
          className="absolute right-0 z-20 mt-2 w-72 max-w-[calc(100vw-2rem)] space-y-2 rounded-2xl border border-zinc-200 bg-white p-3 shadow-lg"
        >
          <Link
            to="/inbox"
            onClick={() => setOpen(false)}
            className="block rounded-xl px-3 py-2.5 text-sm font-semibold text-accent-700 hover:bg-zinc-50"
          >
            All messages
          </Link>

          {state.status === 'loading' && <p role="status" className="px-3 py-2 text-sm text-zinc-500">Loading conversations…</p>}

          {state.status === 'error' && (
            <div role="alert" className="px-3 py-2 text-sm text-zinc-600">
              <p>{getErrorMessage(state.error, 'Could not load your conversations.')}</p>
              <button type="button" onClick={reload} className="mt-1 font-medium text-accent-700 hover:underline">Retry</button>
            </div>
          )}

          {state.status === 'ready' && latest.length === 0 && (
            <p className="px-3 py-2 text-sm text-zinc-500">No conversations yet</p>
          )}

          {latest.map((conversation) => (
            <div key={conversation.id} onClick={() => setOpen(false)}>
              <ConversationRow conversation={conversation} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
