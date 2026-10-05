import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { ProfileView } from '../components/ProfileView'
import { useAuth } from '../lib/auth-context'
import { ApiError, getMyProfile, type Profile } from '../services/profileService'

type ProfileState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; profile: Profile }

export function ProfilePage() {
  const navigate = useNavigate()
  const { setStudent, markProfileMissing } = useAuth()
  const [state, setState] = useState<ProfileState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    getMyProfile()
      .then((profile) => {
        if (!cancelled) setState({ status: 'ready', profile })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (error instanceof ApiError && error.status === 401) {
          // Clear auth state first, otherwise /login's guard bounces back to /feed.
          setStudent(null)
          navigate('/login', { replace: true })
          return
        }
        if (error instanceof ApiError && error.status === 404) {
          // Same for /profile-setup, which only admits students without a profile.
          markProfileMissing()
          navigate('/profile-setup', { replace: true })
          return
        }
        setState({
          status: 'error',
          error: error instanceof ApiError ? error.message : 'Could not load your profile.',
        })
      })
    return () => {
      cancelled = true
    }
    // Fetch once on mount; the auth-context callbacks aren't memoized, so listing them would refetch every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold text-zinc-900">My profile</h1>
        <ProfileView {...state} />
      </main>
    </div>
  )
}
