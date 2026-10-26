import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, resendPin, signup, verifyPin } from '../services/authService'
import { SignupPage } from './SignupPage'

vi.mock('../lib/device', () => ({
  getOrCreateDeviceId: () => 'existing-browser',
}))

vi.mock('../services/authService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/authService')>()),
  signup: vi.fn(),
  verifyPin: vi.fn(),
  resendPin: vi.fn(),
}))

async function submitSignup() {
  const user = userEvent.setup()
  render(<MemoryRouter><SignupPage /></MemoryRouter>)
  await user.type(screen.getByLabelText('Full name'), 'Jane Doe')
  await user.type(screen.getByLabelText('School email'), 'jane@tulane.edu')
  await user.type(screen.getByLabelText('Password'), 'Password123!')
  await user.type(screen.getByLabelText('Confirm password'), 'Password123!')
  await user.click(screen.getByRole('button', { name: 'Continue & Get PIN' }))
  await screen.findByRole('heading', { name: 'Verify your school email' })
  return user
}

describe('SignupPage PIN behavior', () => {
  beforeEach(() => {
    vi.mocked(signup).mockReset()
    vi.mocked(verifyPin).mockReset()
    vi.mocked(resendPin).mockReset()
    vi.mocked(signup).mockResolvedValue({
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
      emailVerified: false, sellingItems: null, dormLocation: null,
    })
  })

  it('moves a valid signup to the PIN step with the email in its subtitle', async () => {
    await submitSignup()

    expect(signup).toHaveBeenCalledWith({
      studentName: 'Jane Doe', email: 'jane@tulane.edu', password: 'Password123!',
    })
    expect(screen.getByText('We generated a 6-digit PIN for jane@tulane.edu')).toBeInTheDocument()
  })

  it('rejects a short PIN before verification', async () => {
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '12345')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Activate Account' }))

    expect(screen.getByText('Enter the 6-digit PIN')).toBeInTheDocument()
    expect(verifyPin).not.toHaveBeenCalled()
  })

  it('strips non-digits from the PIN field', async () => {
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '12ab34')

    expect(screen.getByLabelText('6-Digit Verification PIN')).toHaveValue('1234')
  })

  it('verifies a correct PIN and shows completion', async () => {
    vi.mocked(verifyPin).mockResolvedValue({
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
      emailVerified: true, sellingItems: null, dormLocation: null,
    })
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Activate Account' }))

    expect(await screen.findByRole('heading', { name: 'Account Verified!' })).toBeInTheDocument()
    expect(verifyPin).toHaveBeenCalledWith({
      email: 'jane@tulane.edu', pin: '123456', deviceId: 'existing-browser',
    })
  })

  it('shows an API verification error', async () => {
    vi.mocked(verifyPin).mockRejectedValue(new ApiError(400, 'Incorrect verification PIN'))
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Activate Account' }))

    expect(await screen.findByText('Incorrect verification PIN')).toBeInTheDocument()
  })

  it('shows the fallback for an unexpected verification error', async () => {
    vi.mocked(verifyPin).mockRejectedValue(new Error('Network failed'))
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Activate Account' }))

    expect(await screen.findByText('Failed to verify PIN. Please try again.')).toBeInTheDocument()
  })

  it('resends the PIN and shows confirmation', async () => {
    vi.mocked(resendPin).mockResolvedValue()
    const user = await submitSignup()
    await user.click(screen.getByRole('button', { name: 'Resend PIN' }))

    expect(resendPin).toHaveBeenCalledWith({ email: 'jane@tulane.edu' })
    expect(await screen.findByText('A new 6-digit PIN has been generated!')).toBeInTheDocument()
  })

  it('shows an API resend error', async () => {
    vi.mocked(resendPin).mockRejectedValue(new ApiError(429, 'Please wait before resending'))
    const user = await submitSignup()
    await user.click(screen.getByRole('button', { name: 'Resend PIN' }))

    expect(await screen.findByText('Please wait before resending')).toBeInTheDocument()
  })

  it('returns to the signup form when starting over', async () => {
    const user = await submitSignup()
    await user.click(screen.getByRole('button', { name: 'Start over' }))

    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument()
  })
})
