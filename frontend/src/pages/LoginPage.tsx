import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { validateEmail, validatePassword } from '../lib/validation'
import { ApiError, login, logout, type Student } from '../lib/api'

type FieldErrors = {
  email?: string
  password?: string
}

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)
  const [loggedInStudent, setLoggedInStudent] = useState<Student | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors: FieldErrors = {
      email: validateEmail(email),
      password: validatePassword(password),
    }
    setErrors(nextErrors)
    if (nextErrors.email || nextErrors.password) return

    setStatus('submitting')
    setServerError(null)

    try {
      const student = await login({ email, password })
      setLoggedInStudent(student)
      setStatus('idle')
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.',
      )
      setStatus('error')
    }
  }

  async function handleLogout() {
    await logout()
    setLoggedInStudent(null)
    setEmail('')
    setPassword('')
    setStatus('idle')
  }

  if (loggedInStudent) {
    return (
      <AuthCard
        title="You're logged in"
        subtitle={`Logged in as ${loggedInStudent.studentName}`}
        footer={null}
      >
        <Button type="button" onClick={handleLogout}>
          Log out
        </Button>
      </AuthCard>
    )
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
      {status === 'error' && serverError && <FormAlert kind="error">{serverError}</FormAlert>}

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
