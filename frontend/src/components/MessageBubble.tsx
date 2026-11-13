interface MessageBubbleProps {
  content: string
  createdAt: string
  mine: boolean
  status?: 'sent' | 'sending' | 'failed'
  onRetry?: () => void
}

/**
 * A single chat bubble.
 *
 * Visual direction (Swiss anchor): left-aligned, hairline structure, single coral accent for mine.
 * Mine bubbles on the right in accent colour; theirs on the left in grey. Each shows a time.
 *
 * Accessibility:
 * - <li data-mine> so assistive tech can tell whose message it is.
 * - aria-busy while sending.
 * - "Not sent" text + Retry button when failed.
 */
export function MessageBubble({ content, createdAt, mine, status = 'sent', onRetry }: MessageBubbleProps) {
  return (
    <li
      data-mine={String(mine)}
      aria-busy={status === 'sending'}
      className={`flex flex-col gap-1 ${mine ? 'items-end' : 'items-start'}`}
    >
      <div
        className={`inline-block max-w-[80%] rounded-lg px-3 py-2 text-sm ${
          mine
            ? 'bg-accent-700 text-white'
            : 'bg-zinc-200 text-zinc-900'
        }`}
      >
        {content}
      </div>
      <time
        dateTime={createdAt}
        className={`text-[11px] text-zinc-500 mt-0.5 ${mine ? 'ml-auto' : ''}`}
      >
        {new Date(createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
      </time>
      {status === 'failed' && onRetry && (
        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-xs text-zinc-500">Not sent</span>
          <button
            type="button"
            onClick={onRetry}
            className="text-xs underline underline-offset-2 font-medium text-accent-700 hover:text-accent-800"
          >
            Retry
          </button>
        </div>
      )}
    </li>
  )
}
