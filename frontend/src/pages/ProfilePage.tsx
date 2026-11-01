import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { ProfileEditForm } from '../components/ProfileEditForm'
import { MyListings } from '../components/MyListings'
import { ProfileView } from '../components/ProfileView'
import { FormAlert } from '../components/ui/FormAlert'
import { useAuth } from '../lib/auth-context'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getMyProfile, updateProfile, type ProfileUpdate } from '../services/profileService'

export function ProfilePage() {
  const navigate = useNavigate()
  const { setStudent, markProfileMissing } = useAuth()
  const [editing, setEditing] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  /** Routes away on 401/404 and returns true; returns false for errors the page should show. */
  function redirectOnAuthError(error: unknown): boolean {
    if (hasApiStatus(error, 401)) {
      // Clear auth state first, otherwise /login's guard bounces back to /feed.
      setStudent(null)
      navigate('/login', { replace: true })
      return true
    }
    if (hasApiStatus(error, 404)) {
      // Same for /profile-setup, which only admits students without a profile.
      markProfileMissing()
      navigate('/profile-setup', { replace: true })
      return true
    }
    return false
  }

  const { state, setData } = useAsyncResource(getMyProfile, [], { onError: redirectOnAuthError })

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
      setData(updated)
      setEditing(false)
      setSaveStatus('idle')
      setSaved(true)
    } catch (error) {
      if (redirectOnAuthError(error)) return
      setSaveError(getErrorMessage(error, 'Could not save your profile.'))
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
            profile={state.data}
            status={saveStatus}
            error={saveError}
            onSubmit={(input) => handleSave(state.data.id, input)}
            onCancel={() => setEditing(false)}
          />
        ) : state.status === 'ready' ? (
          <ProfileView status="ready" profile={state.data} onEdit={openEditor} />
        ) : state.status === 'error' ? (
          <ProfileView status="error" error={getErrorMessage(state.error, 'Could not load your profile.')} />
        ) : (
          <ProfileView status="loading" />
        )}
        {state.status === 'ready' && <MyListings sellerId={state.data.studentId} />}
      </main>
    </div>
  )
}
