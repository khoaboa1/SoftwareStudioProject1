import type { ProfileSearchItem } from '../services/profileService'
import { suggestionOptionId } from '../lib/suggestionIds'
export type SuggestionStatus = 'idle' | 'loading' | 'ready' | 'error'
type ProfileSuggestionListProps = {
  listId: string
  status: SuggestionStatus
  suggestions: ProfileSearchItem[]
  activeIndex: number
  onSelect: (profile: ProfileSearchItem) => void
}
/** Dropdown under the search box listing same-school students for an @ query. */
export function ProfileSuggestionList({
  listId,
  status,
  suggestions,
  activeIndex,
  onSelect,
}: ProfileSuggestionListProps) {
  if (status === 'idle') return null
  return (
    <div className="absolute left-0 right-0 top-full z-20 mt-2 overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-lg">
      {status === 'loading' && (
        <p role="status" className="px-4 py-3 text-sm text-zinc-500">
          Searching students…
        </p>
      )}
      {status === 'error' && (
        <p role="alert" className="px-4 py-3 text-sm text-red-700">
          Could not search students
        </p>
      )}
      {status === 'ready' && suggestions.length === 0 && (
        <p className="px-4 py-3 text-sm text-zinc-500">No students found</p>
      )}
      {status === 'ready' && suggestions.length > 0 && (
        <ul
          id={listId}
          role="listbox"
          aria-label="Matching students"
          className="max-h-72 overflow-y-auto py-1"
        >
          {suggestions.map((profile, index) => (
            <li
              key={profile.id}
              id={suggestionOptionId(listId, index)}
              role="option"
              aria-selected={index === activeIndex}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => onSelect(profile)}
              className={`cursor-pointer px-4 py-2.5 ${index === activeIndex ? 'bg-accent-50' : 'hover:bg-zinc-50'}`}
            >
              <span className="block text-sm font-medium text-zinc-900">
                {profile.name}
              </span>
              <span className="block text-xs text-zinc-500">
                <span>{profile.email}</span> ·{' '}
                <span>{profile.schoolDomain}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
