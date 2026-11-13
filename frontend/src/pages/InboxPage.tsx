import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth-context'
import { Button } from '../components/ui/Button'
import { AppHeader } from '../components/AppHeader'
import { FormAlert } from '../components/ui/FormAlert'
import { ConversationRow } from '../components/ConversationRow'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getConversations, sortNewestFirst, toSummary } from '../services/conversationService'

function ConversationRowSkeleton() {
  return (
    <div className="flex items-center gap-3 px-4 py-3.5 rounded-xl border border-zinc-200 bg-white">
      <div className="shrink-0 inline-flex h-9 w-9 items-center justify-center rounded-full bg-zinc-200 animate-pulse" />
      <div className="flex-1 min-w-0 space-y-2">
        <div className="h-4 w-3/4 animate-pulse rounded bg-zinc-200" />
        <div className="flex items-center gap-2">
          <div className="h-4 w-1/3 animate-pulse rounded bg-zinc-200" />
          <div className="h-4 w-1/4 animate-pulse rounded bg-zinc-200" />
        </div>
      </div>
    </div>
  )
}

export function InboxPage() {
  const navigate = useNavigate()
  const { student } = useAuth()
  const { state, reload } = useAsyncResource(() => getConversations(), [], {
    onError: (error) => {
      if (hasApiStatus(error, 401)) {
        navigate('/login')
        return true
      }
      if (hasApiStatus(error, 404)) {
        navigate('/profile-setup')
        return true
      }
      return false
    },
  })

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-4 py-8">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">Inbox</h1>
            <p className="mt-1 text-sm text-zinc-500">
              Your conversations with buyers and sellers
            </p>
          </div>
        </div>

        {state.status === 'loading' && (
          <div role="status" className="space-y-2">
            <span className="sr-only">Loading conversations</span>
            <ConversationRowSkeleton />
            <ConversationRowSkeleton />
            <ConversationRowSkeleton />
            <ConversationRowSkeleton />
          </div>
        )}

        {state.status === 'error' && !hasApiStatus(state.error, 401) && (
          <div role="alert" className="rounded-2xl border border-dashed border-zinc-300 bg-white p-8 text-center">
            <FormAlert kind="error">
              {getErrorMessage(state.error, 'Could not load your conversations. Please try again.')}
            </FormAlert>
            <Button type="button" fullWidth={false} className="mt-4" onClick={reload}>Retry</Button>
          </div>
        )}

        {state.status === 'ready' && state.data.length === 0 && (
          <div className="rounded-2xl border border-zinc-200 bg-white p-10 text-center">
            <p className="text-sm font-medium text-zinc-900">No conversations yet</p>
            <p className="mt-1 text-sm text-zinc-500">
              Start a conversation by messaging a seller from a listing.
            </p>
          </div>
        )}

        {state.status === 'ready' && state.data.length > 0 && student && (
          <div className="space-y-2">
            {sortNewestFirst(state.data).map((c) => toSummary(c, student.id)).map((conversation) => (
              <ConversationRow key={conversation.id} conversation={conversation} />
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
