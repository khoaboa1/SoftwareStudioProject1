import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, resendPin } from '../services/authService'
import { PinVerificationStep } from './PinVerificationStep'

vi.mock('../services/authService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/authService')>()),
  resendPin: vi.fn(),
}))

const props = {
  email: 'jane@tulane.edu',
  title: 'New device verification',
  subtitle: 'Check your email',
  footer: <span>Not your account?</span>,
  helperText: 'Check your school email',
  submitLabel: 'Verify PIN & Trust Device',
  verifyErrorFallback: 'Invalid verification PIN.',
  onVerify: vi.fn<(_pin: string) => Promise<void>>(),
}

function renderStep(overrides: Partial<typeof props> = {}) {
  render(
    <MemoryRouter>
      <PinVerificationStep {...props} {...overrides} />
    </MemoryRouter>,
  )
}

describe('PinVerificationStep', () => {
  beforeEach(() => {
    props.onVerify.mockReset()
    vi.mocked(resendPin).mockReset()
  })

  it('renders the supplied copy', () => {
    renderStep()

    expect(screen.getByRole('heading', { name: 'New device verification' })).toBeInTheDocument()
    expect(screen.getByText('Check your email')).toBeInTheDocument()
    expect(screen.getByText('Not your account?')).toBeInTheDocument()
    expect(screen.getByText('Check your school email')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Verify PIN & Trust Device' })).toBeInTheDocument()
  })

  it('rejects a short PIN before verification', async () => {
    renderStep()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '12345')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(screen.getByText('Enter the 6-digit PIN')).toBeInTheDocument()
    expect(props.onVerify).not.toHaveBeenCalled()
  })

  it('passes a trimmed PIN and shows loading while verification is pending', async () => {
    let resolveVerify: () => void = () => {}
    props.onVerify.mockImplementation(() => new Promise<void>((resolve) => { resolveVerify = resolve }))
    renderStep()
    fireEvent.change(screen.getByLabelText('6-Digit Verification PIN'), {
      target: { value: ' 123456 ' },
    })
    await userEvent.setup().click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(props.onVerify).toHaveBeenCalledWith('123456')
    expect(screen.getByRole('button', { name: 'Please wait…' })).toBeDisabled()
    resolveVerify()
  })

  it('shows an API verification error', async () => {
    props.onVerify.mockRejectedValue(new ApiError(400, 'Incorrect verification PIN'))
    renderStep()
    await userEvent.setup().type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(await screen.findByText('Incorrect verification PIN')).toBeInTheDocument()
  })

  it('shows the supplied fallback for an unexpected verification error', async () => {
    props.onVerify.mockRejectedValue(new Error('Network failed'))
    renderStep()
    await userEvent.setup().type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(await screen.findByText('Invalid verification PIN.')).toBeInTheDocument()
  })

  it('resends the PIN and shows a pending button before confirmation', async () => {
    let resolveResend: () => void = () => {}
    vi.mocked(resendPin).mockImplementation(() => new Promise<void>((resolve) => { resolveResend = resolve }))
    renderStep()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Resend PIN' }))

    expect(resendPin).toHaveBeenCalledWith({ email: 'jane@tulane.edu' })
    expect(screen.getByRole('button', { name: 'Sending...' })).toBeDisabled()
    resolveResend()
    expect(await screen.findByText('A new 6-digit PIN has been generated!')).toBeInTheDocument()
  })
})
