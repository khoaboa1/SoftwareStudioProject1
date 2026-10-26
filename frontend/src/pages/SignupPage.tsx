import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
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
import { ApiError, signup, verifyPin } from '../services/authService'
import { getOrCreateDeviceId } from '../lib/device'

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

  const [step, setStep] = useState<'form' | 'verify_pin' | 'complete'>('form')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
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
    setServerError(null)

    try {
      await signup({ studentName: name, email, password })
      setStatus('idle')
      setStep('verify_pin')
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.',
      )
      setStatus('error')
    }
  }

  if (step === 'complete') {
    return (
      <AuthCard
        title="Account Verified!"
        subtitle="Your .edu school email has been verified and this device is trusted."
        footer={null}
      >
        <div className="flex flex-col gap-4">
          <FormAlert kind="success">
            Welcome to Handoff! Your student verification badge is active.
          </FormAlert>
          <Link to="/login">
            <Button type="button">Go to Log In</Button>
          </Link>
        </div>
      </AuthCard>
    )
  }

  if (step === 'verify_pin') {
    return (
      <PinVerificationStep
        email={email}
        title="Verify your school email"
        subtitle={`We generated a 6-digit PIN for ${email}`}
        helperText="Check your university email (or the backend console during dev/demo)"
        submitLabel="Verify PIN & Activate Account"
        verifyErrorFallback="Failed to verify PIN. Please try again."
        onVerify={async (pin) => {
          await verifyPin({ email, pin, deviceId: getOrCreateDeviceId() })
          setStatus('idle')
          setStep('complete')
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
      {status === 'error' && serverError && <FormAlert kind="error">{serverError}</FormAlert>}

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
          Continue & Get PIN
        </Button>
      </form>
    </AuthCard>
  )
}
