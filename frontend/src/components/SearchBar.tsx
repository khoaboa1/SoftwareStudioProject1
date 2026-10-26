import { MagnifyingGlass, X } from '@phosphor-icons/react'
import { useEffect, useRef, useState, type FormEvent } from 'react'

type SearchBarProps = {
  value: string
  onSearch: (term: string) => void
  debounceMs?: number
}

export function SearchBar({ value, onSearch, debounceMs = 400 }: SearchBarProps) {
  const [text, setText] = useState(value)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const input = useRef<HTMLInputElement>(null)

  // oxlint-disable react-hooks/exhaustive-deps -- Typing changes text; only URL value changes should resync it.
  useEffect(() => {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = null
    // oxlint-disable-next-line react/set-state-in-effect -- This synchronizes an external URL change.
    if (value !== text.trim()) setText(value)
  }, [value])
  // oxlint-enable react-hooks/exhaustive-deps

  useEffect(() => () => {
    if (timer.current !== null) clearTimeout(timer.current)
  }, [])

  function clearTimer() {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = null
  }

  function emit(raw: string) {
    const term = raw.trim()
    if (term !== value) onSearch(term)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearTimer()
    emit(text)
  }

  return (
    <form role="search" onSubmit={handleSubmit} className="relative mb-6">
      <MagnifyingGlass aria-hidden="true" className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-400" />
      <input
        ref={input}
        type="search"
        aria-label="Search listings"
        placeholder="Search listings…"
        maxLength={200}
        value={text}
        onChange={(event) => {
          const next = event.currentTarget.value
          setText(next)
          clearTimer()
          timer.current = setTimeout(() => emit(next), debounceMs)
        }}
        className="w-full rounded-xl border border-zinc-200 bg-white py-3 pl-11 pr-11 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors focus:border-accent-500 focus:ring-2 focus:ring-accent-100 [&::-webkit-search-cancel-button]:hidden"
      />
      {text && (
        <button
          type="button"
          aria-label="Clear search"
          onClick={() => {
            clearTimer()
            setText('')
            emit('')
            input.current?.focus()
          }}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-700"
        >
          <X aria-hidden="true" className="h-5 w-5" />
        </button>
      )}
    </form>
  )
}
