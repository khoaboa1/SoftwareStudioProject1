import { Link } from 'react-router-dom'
import type { ConversationSummary } from '../services/conversationService'

interface ConversationRowProps {
  conversation: ConversationSummary
}

/**
 * A single conversation row, shared by the Message dropdown and the inbox.
 *
 * Three lines, left-aligned: the other person (with the time of the last activity),
 * the item as a smaller subtitle, then a one-line preview of the last message
 * ("You: ..." or "Sam: ..."). The whole row is the link.
 */
export function ConversationRow({ conversation }: ConversationRowProps) {
  const { lastMessage } = conversation
  return (
    <Link
      to={`/conversations/${conversation.id}`}
      className="flex items-start gap-2.5 rounded-xl border border-zinc-200 bg-white px-3 py-2.5 transition-colors hover:bg-zinc-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-500"
      aria-label={`Conversation with ${conversation.otherPartyName} about ${conversation.itemName}`}
    >
      <div className="mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent-100 text-xs font-medium text-accent-900">
        {conversation.otherPartyName.charAt(0).toUpperCase()}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline justify-between gap-2">
          <h3 className="truncate text-sm font-semibold text-zinc-900">{conversation.otherPartyName}</h3>
          <time dateTime={conversation.activityAt} className="whitespace-nowrap font-mono text-[11px] tabular-nums text-zinc-500">
            {new Date(conversation.activityAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
          </time>
        </div>
        <p className="flex items-center gap-1.5 text-xs text-zinc-500">
          <span className="truncate">{conversation.itemName}</span>
          {conversation.sold && (
            <span className="shrink-0 rounded-full bg-zinc-100 px-1.5 py-px text-[10px] font-medium text-zinc-600">Sold</span>
          )}
        </p>
        <p className="mt-1 truncate text-xs text-zinc-700">
          {lastMessage ? (
            <>
              <span className="font-medium">{lastMessage.sender}:</span> {lastMessage.text}
            </>
          ) : (
            <span className="italic text-zinc-400">No messages yet</span>
          )}
        </p>
      </div>
    </Link>
  )
}
