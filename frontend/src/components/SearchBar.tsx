import { MagnifyingGlass, X } from '@phosphor-icons/react'
import { useEffect, useRef, useState, type FormEvent, useId } from 'react'
import {
  searchProfiles,
  type ProfileSearchItem,
} from '../services/profileService'
import {
  ProfileSuggestionList,
  type SuggestionStatus,
} from './ProfileSuggestionList'
import { suggestionOptionId } from '../lib/suggestionIds'

type SearchBarProps = {
  value: string
  onSearch: (term: string) => void
  debounceMs?: number
  onSelectProfile?: (profile: ProfileSearchItem) => void
}

export function SearchBar({
  value,
  onSearch,
  debounceMs = 400,
  onSelectProfile,
}: SearchBarProps) {
  const [text, setText] = useState(value)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const input = useRef<HTMLInputElement>(null)
  const [suggestions, setSuggestions] = useState<ProfileSearchItem[]>([])
  const [suggestStatus, setSuggestStatus] = useState<SuggestionStatus>('idle')
  const [activeIndex, setActiveIndex] = useState(-1)
  const lookupId = useRef(0)
  const listId = useId()
  const mentionsOn = onSelectProfile !== undefined
  const listOpen =
    mentionsOn && text.startsWith('@') && suggestStatus !== 'idle'

  // oxlint-disable react-hooks/exhaustive-deps -- Typing changes text; only URL value changes should resync it.
  useEffect(() => {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = null
    // oxlint-disable-next-line react/set-state-in-effect -- This synchronizes an external URL change.
    if (value !== text.trim()) setText(value)
  }, [value])
  // oxlint-enable react-hooks/exhaustive-deps

  useEffect(
    () => () => {
      if (timer.current !== null) clearTimeout(timer.current)
    },
    []
  )

  function clearTimer() {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = null
  }

  function emit(raw: string) {
    const term = raw.trim()
    if (term !== value) onSearch(term)
  }

  function closeSuggestions() {
    lookupId.current += 1
    setSuggestions([])
    setSuggestStatus('idle')
    setActiveIndex(-1)
  }

  function lookUp(term: string) {
    const id = ++lookupId.current
    setSuggestStatus('loading')
    searchProfiles(term).then(
      (items) => {
        if (id !== lookupId.current) return
        setSuggestions(items)
        setActiveIndex(-1)
        setSuggestStatus('ready')
      },
      () => {
        if (id !== lookupId.current) return
        setSuggestions([])
        setSuggestStatus('error')
      }
    )
  }

  function select(profile: ProfileSearchItem) {
    clearTimer()
    closeSuggestions()
    setText(`@${profile.name}`)
    onSelectProfile?.(profile)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearTimer()
    if (mentionsOn && text.startsWith('@')) return
    emit(text)
  }

  return (
    <form role="search" onSubmit={handleSubmit} className="relative mb-6">
      <MagnifyingGlass
        aria-hidden="true"
        className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-400"
      />
      <input
        ref={input}
        type="search"
        aria-label="Search listings"
        placeholder={
          mentionsOn
            ? 'Search listings, or type @ to find students…'
            : 'Search listings…'
        }
        maxLength={200}
        value={text}
        onChange={(event) => {
          const next = event.currentTarget.value
          setText(next)
          clearTimer()
          if (mentionsOn && next.startsWith('@')) {
            const term = next.slice(1).trim()
            if (!term) {
              closeSuggestions()
              return
            }
            lookupId.current += 1
            setActiveIndex(-1)
            timer.current = setTimeout(() => lookUp(term), debounceMs)
            return
          }
          if (suggestStatus !== 'idle') closeSuggestions()
          timer.current = setTimeout(() => emit(next), debounceMs)
        }}
        onKeyDown={(event) => {
          if (!listOpen) return
          if (event.key === 'ArrowDown' && suggestions.length > 0) {
            event.preventDefault()
            setActiveIndex((index) => (index + 1) % suggestions.length)
          } else if (event.key === 'ArrowUp' && suggestions.length > 0) {
            event.preventDefault()
            setActiveIndex((index) =>
              index <= 0 ? suggestions.length - 1 : index - 1
            )
          } else if (
            event.key === 'Enter' &&
            activeIndex >= 0 &&
            suggestions[activeIndex]
          ) {
            event.preventDefault()
            select(suggestions[activeIndex])
          } else if (event.key === 'Escape') {
            event.preventDefault()
            closeSuggestions()
          }
        }}
        aria-autocomplete={mentionsOn ? 'list' : undefined}
        aria-controls={listOpen ? listId : undefined}
        aria-activedescendant={
          listOpen && activeIndex >= 0
            ? suggestionOptionId(listId, activeIndex)
            : undefined
        }
        className="w-full rounded-xl border border-zinc-200 bg-white py-3 pl-11 pr-11 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors focus:border-accent-500 focus:ring-2 focus:ring-accent-100 [&::-webkit-search-cancel-button]:hidden"
      />
      {text && (
        <button
          type="button"
          aria-label="Clear search"
          onClick={() => {
            clearTimer()
            closeSuggestions()
            setText('')
            emit('')
            input.current?.focus()
          }}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-700"
        >
          <X aria-hidden="true" className="h-5 w-5" />
        </button>
      )}
      {listOpen && (
        <ProfileSuggestionList
          listId={listId}
          status={suggestStatus}
          suggestions={suggestions}
          activeIndex={activeIndex}
          onSelect={select}
        />
      )}
    </form>
  )
}
