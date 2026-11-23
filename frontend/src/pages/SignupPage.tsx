import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { PinVerificationStep } from '../components/PinVerificationStep'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import {
  validateConfirmPassword,
  validateEmail,
  validateName,
  validatePassword,
} from '../lib/validation'
import { signup, verifyPin } from '../services/authService'
import { createAuthFacade } from '../services/authFacade'
import { getErrorMessage, hasApiStatus, isNetworkError } from '../lib/apiError'
import { getOrCreateDeviceId } from '../lib/device'
import { useAuth } from '../lib/auth-context'
type FieldErrors = {
  name?: string
  email?: string
  password?: string
  confirmPassword?: string
}
const DUPLICATE_EMAIL_MESSAGE = 'An account with this email already exists.'
export function SignupPage() {
  const navigate = useNavigate()
  const { setStudent } = useAuth()
  const auth = createAuthFacade(setStudent)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [step, setStep] = useState<'form' | 'verify_pin'>('form')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)
  const [canRetry, setCanRetry] = useState(false)
  const [emailTaken, setEmailTaken] = useState(false)
  async function submitSignup() {
    setStatus('submitting')
    setServerError(null)
    setCanRetry(false)
    setEmailTaken(false)
    try {
      await signup({ studentName: name, email, password })
      setStatus('idle')
      setStep('verify_pin')
    } catch (error) {
      if (hasApiStatus(error, 409)) {
        setErrors((current) => ({ ...current, email: DUPLICATE_EMAIL_MESSAGE }))
        setEmailTaken(true)
        setStatus('idle')
        return
      }
      setServerError(
        getErrorMessage(error, 'Something went wrong. Please try again.')
      )
      setCanRetry(isNetworkError(error))
      setStatus('error')
    }
  }
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
    void submitSignup()
  }
  if (step === 'verify_pin') {
    return (
      <PinVerificationStep
        email={email}
        title="Verify your school email"
        subtitle={`We generated a 6-digit PIN for ${email}`}
        helperText="Check your school email (or backend console during dev/demo)"
        submitLabel="Verify PIN & Activate Account"
        verifyErrorFallback="Failed to verify PIN. Please try again."
        onVerify={async (pin) => {
          await verifyPin({ email, pin, deviceId: getOrCreateDeviceId() })
          // The account is now verified and this device is trusted. Signing in sets the auth cookies.
          const result = await auth.signIn({ email, password })
          navigate(result.requiresPin ? '/login' : '/profile-setup', {
            replace: true,
          })
        }}
        footer={
          <>
            Entered the wrong email?{' '}
            <button
              type="button"
              onClick={() => {
                setStep('form')
                setServerError(null)
                setErrors({})
              }}
              className="font-medium text-accent-700 hover:text-accent-800"
            >
              Start over
            </button>
          </>
        }
      />
    )
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
      {status === 'error' && serverError && (
        <FormAlert
          kind="error"
          action={
            canRetry
              ? { label: 'Try again', onClick: () => void submitSignup() }
              : undefined
          }
        >
          {serverError}
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
          onChange={(e) => {
            setEmail(e.target.value)
            if (emailTaken) setEmailTaken(false)
          }}
        />
        {emailTaken && (
          <p className="-mt-3 text-sm text-zinc-600">
            Already registered?{' '}
            <Link
              to="/login"
              className="font-medium text-accent-700 hover:text-accent-800"
            >
              Log in instead
            </Link>
          </p>
        )}
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
          Continue & Get PIN
        </Button>
      </form>
    </AuthCard>
  )
}
