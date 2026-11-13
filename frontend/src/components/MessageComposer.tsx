import { useCallback, useRef, useState } from 'react'

interface MessageComposerProps {
  onSend: (content: string) => void
  sending: boolean
}

const MAX_LENGTH = 1000

/**
 * Message input box with a Send button.
 *
 * Swiss anchor: neutral zinc surface, single coral accent on the primary Send button,
 * left-aligned text.
 *
 * Accessibility:
 * - textarea labelled "Message" via aria-label.
 * - Send button disabled when empty/only-spaces or while sending.
 * - Enter sends; Shift+Enter starts a new line.
 */
export function MessageComposer({ onSend, sending }: MessageComposerProps) {
  const [value, setValue] = useState('')
  const boxRef = useRef<HTMLTextAreaElement>(null)

  const maxExceeded = value.length > MAX_LENGTH

  const handleChange = useCallback((event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setValue(event.target.value)
  }, [])

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        const trimmed = value.trim()
        if (trimmed && !maxExceeded && !sending) {
          onSend(trimmed)
          setValue('')
          boxRef.current?.focus()
        }
      }
    },
    [value, maxExceeded, sending, onSend],
  )

  const handleSubmit = useCallback(
    (event: React.FormEvent) => {
      event.preventDefault()
      const trimmed = value.trim()
      if (trimmed && !maxExceeded && !sending) {
        onSend(trimmed)
        setValue('')
        boxRef.current?.focus()
      }
    },
    [value, maxExceeded, sending, onSend],
  )

  const disabled = !value.trim() || maxExceeded || sending

  return (
    <form onSubmit={handleSubmit} className="flex items-end gap-2 rounded-lg border border-zinc-200 bg-white p-3">
      <textarea
        ref={boxRef}
        id="message"
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={sending}
        aria-label="Message"
        className="flex-1 min-h-[44px] resize-none border-none bg-transparent text-sm text-zinc-900 focus-visible:outline-none"
        placeholder="Write a message…"
      />
      {maxExceeded && (
        <span className="text-xs text-zinc-500">Message must be at most 1000 characters</span>
      )}
      <button
        type="submit"
        disabled={disabled}
        className="inline-flex items-center justify-center rounded-lg bg-accent-700 px-4 py-2.5 text-[15px] font-medium text-white transition-all hover:bg-accent-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-500 disabled:cursor-not-allowed disabled:opacity-60 whitespace-nowrap"
      >
        Send
      </button>
    </form>
  )
}
