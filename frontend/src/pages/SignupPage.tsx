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
import { ApiError, resendPin, signup, verifyPin } from '../services/authService'
import { getOrCreateDeviceId } from '../lib/device'

type FieldErrors = {
  name?: string
  email?: string
  password?: string
  confirmPassword?: string
  pin?: string
}

export function SignupPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [pin, setPin] = useState('')

  const [step, setStep] = useState<'form' | 'verify_pin' | 'complete'>('form')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [resendStatus, setResendStatus] = useState<'idle' | 'sending' | 'sent'>('idle')
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

  async function handleVerifyPin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!pin.trim() || pin.trim().length !== 6) {
      setErrors({ pin: 'Enter the 6-digit PIN' })
      return
    }

    setStatus('submitting')
    setServerError(null)

    try {
      await verifyPin({
        email,
        pin: pin.trim(),
        deviceId: getOrCreateDeviceId(),
      })
      setStatus('idle')
      setStep('complete')
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Failed to verify PIN. Please try again.',
      )
      setStatus('error')
    }
  }

  async function handleResendPin() {
    setResendStatus('sending')
    setServerError(null)
    try {
      await resendPin({ email })
      setResendStatus('sent')
      setTimeout(() => setResendStatus('idle'), 4000)
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Failed to resend PIN.',
      )
      setResendStatus('idle')
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
      <AuthCard
        title="Verify your school email"
        subtitle={`We generated a 6-digit PIN for ${email}`}
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
      >
        {serverError && <FormAlert kind="error">{serverError}</FormAlert>}
        {resendStatus === 'sent' && (
          <FormAlert kind="success">A new 6-digit PIN has been generated!</FormAlert>
        )}

        <form onSubmit={handleVerifyPin} noValidate className="flex flex-col gap-5">
          <TextField
            label="6-Digit Verification PIN"
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="123456"
            helperText="Check your university email (or the backend console during dev/demo)"
            error={errors.pin}
            value={pin}
            onChange={(e) => {
              setPin(e.target.value.replace(/\D/g, ''))
              if (errors.pin) setErrors({})
            }}
          />

          <Button type="submit" loading={status === 'submitting'}>
            Verify PIN & Activate Account
          </Button>

          <div className="flex items-center justify-between text-xs text-zinc-500 pt-2">
            <span>Didn't receive it?</span>
            <button
              type="button"
              onClick={handleResendPin}
              disabled={resendStatus === 'sending'}
              className="font-medium text-accent-700 hover:text-accent-800 underline disabled:opacity-50"
            >
              {resendStatus === 'sending' ? 'Sending...' : 'Resend PIN'}
            </button>
          </div>
        </form>
      </AuthCard>
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
