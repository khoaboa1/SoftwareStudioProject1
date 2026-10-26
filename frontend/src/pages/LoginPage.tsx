import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { PinVerificationStep } from '../components/PinVerificationStep'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { validateEmail, validatePassword } from '../lib/validation'
import { ApiError } from '../services/authService'
import { createAuthFacade } from '../services/authFacade'
import { useAuth } from '../lib/auth-context'

type FieldErrors = {
  email?: string
  password?: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const { setStudent } = useAuth()
  const auth = createAuthFacade(setStudent)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const [step, setStep] = useState<'form' | 'verify_pin'>('form')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)
  const [promptMessage, setPromptMessage] = useState<string | null>(null)

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
      const res = await auth.signIn({ email, password })

      if (res.requiresPin) {
        setStep('verify_pin')
        setPromptMessage(res.message || 'New device detected. Enter your 6-digit PIN.')
        setStatus('idle')
      } else {
        navigate('/feed')
      }
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.',
      )
      setStatus('error')
    }
  }

  if (step === 'verify_pin') {
    return (
      <PinVerificationStep
        email={email}
        title="New device verification"
        subtitle={promptMessage || `Enter the 6-digit PIN sent to ${email}`}
        helperText="Check your school email (or backend console during dev/demo)"
        submitLabel="Verify PIN & Trust Device"
        verifyErrorFallback="Invalid verification PIN."
        onVerify={async (pin) => {
          await auth.verifySignIn({ email, pin })
          navigate('/feed')
        }}
        footer={
          <>
            Not your account?{' '}
            <button
              type="button"
              onClick={() => {
                setStep('form')
                setServerError(null)
                setErrors({})
                setPassword('')
              }}
              className="font-medium text-accent-700 hover:text-accent-800"
            >
              Back to login
            </button>
          </>
        }
      />
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
