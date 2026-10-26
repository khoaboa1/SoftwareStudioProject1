import { useId, useState, type FormEvent } from 'react'
import { Button } from './ui/Button'
import { FormAlert } from './ui/FormAlert'
import { TextField } from './ui/TextField'
import type { Profile, ProfileUpdate } from '../services/profileService'

/** Matches the backend's @Size(max = 1000) on Profile.bio. */
const BIO_MAX_LENGTH = 1000

type FieldErrors = Partial<Record<keyof ProfileUpdate, string>>

type ProfileEditFormProps = {
  profile: Profile
  status: 'idle' | 'submitting' | 'error'
  error: string | null
  onSubmit: (input: ProfileUpdate) => void
  onCancel: () => void
}

export function ProfileEditForm({ profile, status, error, onSubmit, onCancel }: ProfileEditFormProps) {
  const bioId = useId()
  const [name, setName] = useState(profile.name)
  const [major, setMajor] = useState(profile.major)
  const [bio, setBio] = useState(profile.bio ?? '')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const input = { name: name.trim(), major: major.trim(), bio: bio.trim() }
    const errors: FieldErrors = {}
    if (!input.name) errors.name = 'Name is required'
    if (!input.major) errors.major = 'Major is required'
    if (input.bio.length > BIO_MAX_LENGTH) errors.bio = `Bio cannot exceed ${BIO_MAX_LENGTH} characters`

    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    onSubmit(input)
  }

  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm">
      <h2 className="mb-5 text-lg font-semibold text-zinc-900">Edit profile</h2>

      {status === 'error' && error && <FormAlert kind="error">{error}</FormAlert>}

      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
        <TextField
          label="Name"
          type="text"
          autoComplete="name"
          error={fieldErrors.name}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <TextField
          label="Major"
          type="text"
          autoComplete="off"
          error={fieldErrors.major}
          value={major}
          onChange={(e) => setMajor(e.target.value)}
        />

        <div className="flex flex-col gap-1.5">
          <label htmlFor={bioId} className="text-sm font-medium text-zinc-800">
            Bio
          </label>
          <textarea
            id={bioId}
            rows={4}
            placeholder="A short note about yourself"
            aria-invalid={Boolean(fieldErrors.bio)}
            aria-describedby={`${bioId}-hint`}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            className={`w-full resize-y rounded-lg border px-3.5 py-2.5 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors ${
              fieldErrors.bio
                ? 'border-red-400 focus:border-red-500 focus:ring-2 focus:ring-red-100'
                : 'border-zinc-300 focus:border-accent-500 focus:ring-2 focus:ring-accent-100'
            }`}
          />
          <div id={`${bioId}-hint`} className="flex justify-between gap-2 text-sm">
            <span className="text-red-600">{fieldErrors.bio}</span>
            <span className={bio.trim().length > BIO_MAX_LENGTH ? 'text-red-600' : 'text-zinc-500'}>
              {bio.trim().length}/{BIO_MAX_LENGTH}
            </span>
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg px-4 py-2 text-sm font-medium text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900"
          >
            Cancel
          </button>
          <Button type="submit" fullWidth={false} loading={status === 'submitting'} className="px-4 py-2 text-sm">
            Save changes
          </Button>
        </div>
      </form>
    </section>
  )
}
