import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import {
  validateConfirmPassword,
  validateEmail,
  validateName,
  validatePassword,
} from '../lib/validation'

type FieldErrors = {
  name?: string
  email?: string
  password?: string
  confirmPassword?: string
}

export function SignupPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'success'>('idle')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors: FieldErrors = {
      name: validateName(name),
      email: validateEmail(email),
      password: validatePassword(password),
      confirmPassword: validateConfirmPassword(confirmPassword, password),
    }
    setErrors(nextErrors)
    if (Object.values(nextErrors).some(Boolean)) return

    setStatus('submitting')

    // TODO: replace with a real call once the backend exposes signup +
    // .edu verification endpoints (see SSP1-5 for current backend scope).
    window.setTimeout(() => {
      setStatus('success')
    }, 800)
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Verify with your .edu email to join your campus marketplace"
      footer={
        <>
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-medium text-accent-700 hover:text-accent-800"
          >
            Log in
          </Link>
        </>
      }
    >
      {status === 'success' && (
        <FormAlert kind="success">
          Account details captured locally — sign-up isn't wired to a
          backend yet. This is a UI skeleton.
        </FormAlert>
      )}

      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
        <TextField
          label="Full name"
          type="text"
          autoComplete="name"
          placeholder="Jane Doe"
          error={errors.name}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <TextField
          label="School email"
          type="email"
          autoComplete="email"
          placeholder="you@university.edu"
          helperText={errors.email ? undefined : 'Must be a .edu email'}
          error={errors.email}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="••••••••"
          helperText={errors.password ? undefined : 'At least 8 characters'}
          error={errors.password}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="••••••••"
          error={errors.confirmPassword}
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
        />

        <Button type="submit" loading={status === 'submitting'}>
          Create account
        </Button>
      </form>
    </AuthCard>
  )
}
