import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { validateEmail, validatePassword } from '../lib/validation'
import { ApiError, login, resendPin, verifyPin } from '../services/authService'
import { getOrCreateDeviceId } from '../lib/device'
import { useAuth } from '../lib/auth-context'

type FieldErrors = {
  email?: string
  password?: string
  pin?: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const { setStudent } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pin, setPin] = useState('')

  const [step, setStep] = useState<'form' | 'verify_pin'>('form')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [resendStatus, setResendStatus] = useState<'idle' | 'sending' | 'sent'>('idle')
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
      const res = await login({
        email,
        password,
        deviceId: getOrCreateDeviceId(),
      })

      if (res.requiresPin) {
        setStep('verify_pin')
        setPromptMessage(res.message || 'New device detected. Enter your 6-digit PIN.')
        setStatus('idle')
      } else {
        if (res.id && res.studentName && res.email) {
          setStudent({
            id: res.id,
            studentName: res.studentName,
            email: res.email,
            emailVerified: res.emailVerified ?? true,
            sellingItems: res.sellingItems ?? null,
            dormLocation: res.dormLocation ?? null,
          })
        }
        navigate('/feed')
      }
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
      const student = await verifyPin({
        email,
        pin: pin.trim(),
        deviceId: getOrCreateDeviceId(),
      })
      setStudent(student)
      navigate('/feed')
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Invalid verification PIN.',
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

  if (step === 'verify_pin') {
    return (
      <AuthCard
        title="New device verification"
        subtitle={promptMessage || `Enter the 6-digit PIN sent to ${email}`}
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
            helperText="Check your school email (or backend console during dev/demo)"
            error={errors.pin}
            value={pin}
            onChange={(e) => {
              setPin(e.target.value.replace(/\D/g, ''))
              if (errors.pin) setErrors({})
            }}
          />

          <Button type="submit" loading={status === 'submitting'}>
            Verify PIN & Trust Device
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
