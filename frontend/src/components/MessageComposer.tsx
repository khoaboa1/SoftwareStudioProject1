import { useCallback, useRef, useState } from 'react'

interface MessageComposerProps {
  onSend: (content: string) => void
  sending: boolean
  value?: string
  onChange?: (text: string) => void
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
 * - Send button disabled when empty/only-spaces or while sending. The textarea
 *   itself stays enabled so focus is never stolen while a message is in flight.
 * - Enter sends; Shift+Enter starts a new line.
 *
 * When `value`/`onChange` are supplied the composer is controlled (the page owns
 * the text, which lets it survive a rejected send). Otherwise it manages its own
 * state.
 */
export function MessageComposer({ onSend, sending, value, onChange }: MessageComposerProps) {
  const [internalValue, setInternalValue] = useState('')
  const boxRef = useRef<HTMLTextAreaElement>(null)

  const isControlled = value !== undefined
  const displayValue = isControlled ? value : internalValue
  const maxExceeded = displayValue.length > MAX_LENGTH

  const handleChange = useCallback((event: React.ChangeEvent<HTMLTextAreaElement>) => {
    const next = event.target.value
    if (!isControlled) setInternalValue(next)
    onChange?.(next)
  }, [isControlled, onChange])

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        const trimmed = displayValue.trim()
        if (trimmed && !maxExceeded && !sending) {
          onSend(trimmed)
          if (!isControlled) setInternalValue('')
          onChange?.('')
        }
      }
    },
    [displayValue, maxExceeded, sending, onSend, isControlled, onChange],
  )

  const handleSubmit = useCallback(
    (event: React.FormEvent) => {
      event.preventDefault()
      const trimmed = displayValue.trim()
      if (trimmed && !maxExceeded && !sending) {
        onSend(trimmed)
        if (!isControlled) setInternalValue('')
        onChange?.('')
      }
    },
    [displayValue, maxExceeded, sending, onSend, isControlled, onChange],
  )

  const disabled = !displayValue.trim() || maxExceeded || sending

  return (
    <form onSubmit={handleSubmit} className="flex items-end gap-2 rounded-lg border border-zinc-200 bg-white p-3">
      <textarea
        ref={boxRef}
        id="message"
        value={displayValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
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
