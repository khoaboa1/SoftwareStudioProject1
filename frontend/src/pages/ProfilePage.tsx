import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { ProfileEditForm } from '../components/ProfileEditForm'
import { MyListings } from '../components/MyListings'
import { ProfileView } from '../components/ProfileView'
import { FormAlert } from '../components/ui/FormAlert'
import { useAuth } from '../lib/auth-context'
import { ApiError, getMyProfile, updateProfile, type Profile, type ProfileUpdate } from '../services/profileService'

type ProfileState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; profile: Profile }

export function ProfilePage() {
  const navigate = useNavigate()
  const { setStudent, markProfileMissing } = useAuth()
  const [state, setState] = useState<ProfileState>({ status: 'loading' })
  const [editing, setEditing] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  /** Routes away on 401/404 and returns true; returns false for errors the page should show. */
  function redirectOnAuthError(error: unknown): boolean {
    if (error instanceof ApiError && error.status === 401) {
      // Clear auth state first, otherwise /login's guard bounces back to /feed.
      setStudent(null)
      navigate('/login', { replace: true })
      return true
    }
    if (error instanceof ApiError && error.status === 404) {
      // Same for /profile-setup, which only admits students without a profile.
      markProfileMissing()
      navigate('/profile-setup', { replace: true })
      return true
    }
    return false
  }

  useEffect(() => {
    let cancelled = false
    getMyProfile()
      .then((profile) => {
        if (!cancelled) setState({ status: 'ready', profile })
      })
      .catch((error: unknown) => {
        if (cancelled || redirectOnAuthError(error)) return
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

  function openEditor() {
    setSaved(false)
    setSaveStatus('idle')
    setSaveError(null)
    setEditing(true)
  }

  async function handleSave(profileId: number, input: ProfileUpdate) {
    setSaveStatus('submitting')
    setSaveError(null)
    try {
      const updated = await updateProfile(profileId, input)
      setState({ status: 'ready', profile: updated })
      setEditing(false)
      setSaveStatus('idle')
      setSaved(true)
    } catch (error) {
      if (redirectOnAuthError(error)) return
      setSaveError(error instanceof ApiError ? error.message : 'Could not save your profile.')
      setSaveStatus('error')
    }
  }

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold text-zinc-900">My profile</h1>
        {saved && <FormAlert kind="success">Profile updated.</FormAlert>}
        {state.status === 'ready' && editing ? (
          <ProfileEditForm
            profile={state.profile}
            status={saveStatus}
            error={saveError}
            onSubmit={(input) => handleSave(state.profile.id, input)}
            onCancel={() => setEditing(false)}
          />
        ) : state.status === 'ready' ? (
          <ProfileView status="ready" profile={state.profile} onEdit={openEditor} />
        ) : (
          <ProfileView {...state} />
        )}
        {state.status === 'ready' && <MyListings sellerId={state.profile.studentId} />}
      </main>
    </div>
  )
}
