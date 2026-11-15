import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NetworkError } from '../services/httpClient'
import { ApiError, resendPin, signup, verifyPin } from '../services/authService'
import { SignupPage } from './SignupPage'

const { setStudent } = vi.hoisted(() => ({ setStudent: vi.fn() }))

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ setStudent }),
}))

vi.mock('../lib/device', () => ({
  getOrCreateDeviceId: () => 'existing-browser',
}))

vi.mock('../services/authService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/authService')>()),
  signup: vi.fn(),
  verifyPin: vi.fn(),
  resendPin: vi.fn(),
}))

async function fillAndSubmitSignup() {
  const user = userEvent.setup()
  render(
    <MemoryRouter initialEntries={['/signup']}>
      <Routes>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/profile-setup" element={<p>Profile setup destination</p>} />
        <Route path="/login" element={<p>Login destination</p>} />
      </Routes>
    </MemoryRouter>,
  )
  await user.type(screen.getByLabelText('Full name'), 'Jane Doe')
  await user.type(screen.getByLabelText('School email'), 'jane@tulane.edu')
  await user.type(screen.getByLabelText('Password'), 'Password123!')
  await user.type(screen.getByLabelText('Confirm password'), 'Password123!')
  await user.click(screen.getByRole('button', { name: 'Continue & Get PIN' }))
  return user
}
async function submitSignup() {
  const user = await fillAndSubmitSignup()
  await screen.findByRole('heading', { name: 'Verify your school email' })
  return user
}

describe('SignupPage PIN behavior', () => {
  beforeEach(() => {
    setStudent.mockReset()
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

  it('verifies a correct PIN, signs the student in and opens profile setup', async () => {
    const student = {
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
      emailVerified: true, sellingItems: null, dormLocation: null,
    }
    vi.mocked(verifyPin).mockResolvedValue(student)
    const user = await submitSignup()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Activate Account' }))
    expect(await screen.findByText('Profile setup destination')).toBeInTheDocument()
    expect(setStudent).toHaveBeenCalledWith(student)
    expect(verifyPin).toHaveBeenCalledWith({
      email: 'jane@tulane.edu', pin: '123456', deviceId: 'existing-browser',
    })
  })

  it('shows a duplicate email on the email field with a link to log in', async () => {
    vi.mocked(signup).mockRejectedValueOnce(new ApiError(409, 'Email already registered'))
    const user = await fillAndSubmitSignup()
    expect(await screen.findByText('An account with this email already exists.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Log in instead' }))
    expect(await screen.findByText('Login destination')).toBeInTheDocument()
  })

  it('offers a retry when the server cannot be reached', async () => {
    vi.mocked(signup).mockRejectedValueOnce(new NetworkError())
    const user = await fillAndSubmitSignup()
    expect(await screen.findByText("Can't reach the server. Check your connection and try again."))
    .toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Verify your school email' })).toBeInTheDocument()
    expect(signup).toHaveBeenCalledTimes(2)
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
