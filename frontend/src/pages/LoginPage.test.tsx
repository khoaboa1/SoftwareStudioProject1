import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, login, verifyPin } from '../services/authService'
import { LoginPage } from './LoginPage'

const { setStudent } = vi.hoisted(() => ({ setStudent: vi.fn() }))

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ setStudent }),
}))

vi.mock('../lib/device', () => ({
  getOrCreateDeviceId: () => 'existing-browser',
}))

vi.mock('../services/authService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/authService')>()),
  login: vi.fn(),
  verifyPin: vi.fn(),
}))

async function submitLogin() {
  const user = userEvent.setup()
  render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/feed" element={<p>Marketplace destination</p>} />
      </Routes>
    </MemoryRouter>,
  )
  await user.type(screen.getByLabelText('School email'), 'jane@tulane.edu')
  await user.type(screen.getByLabelText('Password'), 'Password123!')
  await user.click(screen.getByRole('button', { name: 'Log in' }))
  return user
}

describe('LoginPage authentication behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(login).mockReset()
    vi.mocked(verifyPin).mockReset()
  })

  it('keeps password-login defaults, device identity, auth update, and navigation', async () => {
    vi.mocked(login).mockResolvedValue({
      requiresPin: false, message: 'Login successful',
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
    })

    await submitLogin()

    expect(await screen.findByText('Marketplace destination')).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({
      email: 'jane@tulane.edu', password: 'Password123!', deviceId: 'existing-browser',
    })
    expect(setStudent).toHaveBeenCalledWith({
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
      emailVerified: true, sellingItems: null, dormLocation: null,
    })
  })

  it('waits for PIN verification before updating auth and navigating', async () => {
    vi.mocked(login).mockResolvedValue({ requiresPin: true, message: 'Check your email' })
    const student = {
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
      emailVerified: true, sellingItems: null, dormLocation: null,
    }
    vi.mocked(verifyPin).mockResolvedValue(student)

    const user = await submitLogin()

    expect(await screen.findByText('Check your email')).toBeInTheDocument()
    expect(setStudent).not.toHaveBeenCalled()
    await user.type(screen.getByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(await screen.findByText('Marketplace destination')).toBeInTheDocument()
    expect(verifyPin).toHaveBeenCalledWith({
      email: 'jane@tulane.edu', pin: '123456', deviceId: 'existing-browser',
    })
    expect(setStudent).toHaveBeenCalledWith(student)
  })

  it('keeps a failed login on the form without changing authentication', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, 'Invalid credentials'))

    await submitLogin()

    expect(await screen.findByText('Invalid credentials')).toBeInTheDocument()
    expect(setStudent).not.toHaveBeenCalled()
    expect(screen.queryByText('Marketplace destination')).not.toBeInTheDocument()
  })

  it('keeps a failed PIN verification on the verification form', async () => {
    vi.mocked(login).mockResolvedValue({ requiresPin: true, message: 'Check your email' })
    vi.mocked(verifyPin).mockRejectedValue(new ApiError(400, 'Incorrect verification PIN'))

    const user = await submitLogin()
    await user.type(await screen.findByLabelText('6-Digit Verification PIN'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify PIN & Trust Device' }))

    expect(await screen.findByText('Incorrect verification PIN')).toBeInTheDocument()
    expect(setStudent).not.toHaveBeenCalled()
    expect(screen.queryByText('Marketplace destination')).not.toBeInTheDocument()
  })
})
