import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { validateEmail, validatePassword } from '../lib/validation'

type FieldErrors = {
  email?: string
  password?: string
}

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors: FieldErrors = {
      email: validateEmail(email),
      password: validatePassword(password),
    }
    setErrors(nextErrors)
    if (nextErrors.email || nextErrors.password) return

    setStatus('submitting')

    // TODO: replace with a real call once the backend exposes an auth endpoint
    // (see SSP1-5 — only a read-only Student listing API exists so far).
    window.setTimeout(() => {
      setStatus('error')
    }, 800)
  }

  return (
    <AuthCard
      title="Log in to Handoff"
      subtitle="Buy and sell with verified students on your campus"
      footer={
        <>
          New to Handoff?{' '}
          <Link
            to="/signup"
            className="font-medium text-accent-700 hover:text-accent-800"
          >
            Sign up
          </Link>
        </>
      }
    >
      {status === 'error' && (
        <FormAlert kind="error">
          We couldn't log you in — login isn't connected yet. This is a UI
          skeleton.
        </FormAlert>
      )}

      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
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
          autoComplete="current-password"
          placeholder="••••••••"
          error={errors.password}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <Button type="submit" loading={status === 'submitting'}>
          Log in
        </Button>
      </form>
    </AuthCard>
  )
}
