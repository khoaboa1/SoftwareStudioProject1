import { Link } from 'react-router-dom'
import type { ConversationSummary } from '../services/conversationService'

interface ConversationRowProps {
  conversation: ConversationSummary
}

/**
 * A single conversation row in the inbox list.
 *
 * Visual direction (Swiss anchor): hairline divider, left-aligned typography,
 * single deliberate accent (coral) on the avatar, neutral zinc surface.
 *
 * Layout: identifying content (name) leads, metadata (item, timestamp) trails.
 * Hit area exceeds 44x44px touch target. Focus ring for keyboard users.
 * Truncated item name opens the full conversation on click (way back).
 */
export function ConversationRow({ conversation }: ConversationRowProps) {
  return (
    <Link
      to={`/conversations/${conversation.id}`}
      className="flex items-center gap-3 px-4 py-3.5 rounded-xl border border-zinc-200 bg-white transition-colors hover:bg-zinc-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-500"
      aria-label={`Conversation with ${conversation.otherPartyName} about ${conversation.itemName}`}
    >
      <div className="shrink-0 inline-flex h-9 w-9 items-center justify-center rounded-full bg-accent-100 text-accent-900 text-xs font-medium">
        {conversation.otherPartyName.charAt(0).toUpperCase()}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-zinc-900 truncate">
            {conversation.otherPartyName}
          </h3>
          <time dateTime={conversation.createdAt} className="text-xs text-zinc-500 font-mono tabular-nums whitespace-nowrap">
            {new Date(conversation.createdAt).toLocaleDateString(undefined, {
              month: 'short',
              day: 'numeric',
            })}
          </time>
        </div>
        <p className="mt-0.5 flex items-center gap-2 text-sm text-zinc-600">
          <span className="truncate">{conversation.itemName}</span>
          {conversation.sold && (
            <span className="shrink-0 rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-600">Sold</span>
          )}
        </p>
      </div>
    </Link>
  )
}
