import { GraduationCap, Buildings } from '@phosphor-icons/react'
import { FormAlert } from './ui/FormAlert'
import type { Profile } from '../services/profileService'

type ProfileViewProps =
  | { status: 'loading' }
  | { status: 'error'; error?: string }
  | { status: 'ready'; profile: Profile }

function initialsOf(name: string): string {
  const words = name.trim().split(/\s+/)
  const first = words[0]?.[0] ?? ''
  const last = words.length > 1 ? words[words.length - 1][0] : ''
  return (first + last).toUpperCase()
}

function formatMemberSince(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
}

export function ProfileView(props: ProfileViewProps) {
  if (props.status === 'loading') {
    return (
      <div
        aria-label="Loading profile"
        aria-busy="true"
        className="flex animate-pulse items-center gap-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm"
      >
        <div className="h-20 w-20 shrink-0 rounded-full bg-zinc-200" />
        <div className="flex flex-1 flex-col gap-3">
          <div className="h-5 w-40 rounded bg-zinc-200" />
          <div className="h-4 w-56 rounded bg-zinc-100" />
        </div>
      </div>
    )
  }

  if (props.status === 'error') {
    return <FormAlert kind="error">{props.error ?? 'Something went wrong loading this profile.'}</FormAlert>
  }

  const { profile } = props
  const bio = profile.bio?.trim()

  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
        <div
          aria-hidden="true"
          className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-accent-100 text-2xl font-semibold text-accent-800"
        >
          {initialsOf(profile.name)}
        </div>
        <div className="flex flex-col gap-1.5">
          <h2 className="text-2xl font-semibold text-zinc-900">{profile.name}</h2>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-sm text-zinc-600">
            <span className="flex items-center gap-1.5">
              <GraduationCap className="h-4 w-4" aria-hidden="true" />
              <span>{profile.major}</span>
            </span>
            <span className="flex items-center gap-1.5">
              <Buildings className="h-4 w-4" aria-hidden="true" />
              <span className="rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
                {profile.schoolDomain}
              </span>
            </span>
          </div>
          <p className="text-xs text-zinc-400">Member since {formatMemberSince(profile.createdAt)}</p>
        </div>
      </div>

      <div className="mt-6 border-t border-zinc-100 pt-5">
        <h3 className="mb-2 text-sm font-medium text-zinc-900">About</h3>
        {bio ? (
          <p className="whitespace-pre-line text-sm text-zinc-600">{bio}</p>
        ) : (
          <p className="text-sm italic text-zinc-400">No bio yet.</p>
        )}
      </div>
    </section>
  )
}
