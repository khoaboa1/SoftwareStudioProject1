import { type FormEvent, type ReactNode, useState } from 'react'
import { resendPin } from '../services/authService'
import { getErrorMessage } from '../lib/apiError'
import { AuthCard } from './AuthCard'
import { Button } from './ui/Button'
import { FormAlert } from './ui/FormAlert'
import { TextField } from './ui/TextField'

type PinVerificationStepProps = {
  email: string
  title: string
  subtitle: string
  footer: ReactNode
  helperText: string
  submitLabel: string
  verifyErrorFallback: string
  onVerify: (pin: string) => Promise<void>
}

export function PinVerificationStep({
  email, title, subtitle, footer, helperText, submitLabel, verifyErrorFallback, onVerify,
}: PinVerificationStepProps) {
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState<string | undefined>()
  const [status, setStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [resendStatus, setResendStatus] = useState<'idle' | 'sending' | 'sent'>('idle')
  const [serverError, setServerError] = useState<string | null>(null)

  async function handleVerifyPin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!pin.trim() || pin.trim().length !== 6) {
      setPinError('Enter the 6-digit PIN')
      return
    }

    setStatus('submitting')
    setServerError(null)

    try {
      await onVerify(pin.trim())
    } catch (error) {
      setServerError(
        getErrorMessage(error, verifyErrorFallback),
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
        getErrorMessage(error, 'Failed to resend PIN.'),
      )
      setResendStatus('idle')
    }
  }

  return (
    <AuthCard title={title} subtitle={subtitle} footer={footer}>
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
          helperText={helperText}
          error={pinError}
          value={pin}
          onChange={(e) => {
            setPin(e.target.value.replace(/\D/g, ''))
            if (pinError) setPinError(undefined)
          }}
        />

        <Button type="submit" loading={status === 'submitting'}>
          {submitLabel}
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
