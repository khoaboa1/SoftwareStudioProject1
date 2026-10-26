import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { useAuth } from '../lib/auth-context'
import { ApiError, createProfile } from '../services/profileService'

export function ProfileSetupPage() {
  const navigate = useNavigate()
  const { student, markProfileComplete } = useAuth()
  const [major, setMajor] = useState('')
  const [bio, setBio] = useState('')
  const [majorError, setMajorError] = useState<string | undefined>(undefined)
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!major.trim()) {
      setMajorError('Major is required')
      return
    }
    setMajorError(undefined)

    setStatus('submitting')
    setServerError(null)

    try {
      await createProfile({
        name: student?.studentName ?? '',
        major: major.trim(),
        bio: bio.trim() || undefined,
      })
      markProfileComplete()
      navigate('/feed')
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.',
      )
      setStatus('error')
    }
  }

  return (
    <AuthCard
      title="Finish setting up your profile"
      subtitle="Tell us your major so we can connect you to your campus marketplace"
      footer={null}
    >
      {status === 'error' && serverError && <FormAlert kind="error">{serverError}</FormAlert>}

      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
        <TextField
          label="Academic major"
          type="text"
          autoComplete="off"
          placeholder="Computer Science"
          error={majorError}
          value={major}
          onChange={(e) => setMajor(e.target.value)}
        />

        <TextField
          label="Bio (optional)"
          type="text"
          autoComplete="off"
          placeholder="A short note about yourself"
          value={bio}
          onChange={(e) => setBio(e.target.value)}
        />

        <Button type="submit" loading={status === 'submitting'}>
          Continue to marketplace
        </Button>
      </form>
    </AuthCard>
  )
}
