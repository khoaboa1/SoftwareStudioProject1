import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './auth-context'
import { onSessionExpired } from '../services/httpClient'

vi.mock('../services/authService', () => ({
  getCurrentStudent: vi.fn().mockResolvedValue({
    id: 1,
    studentName: 'Jane Doe',
    email: 'jane@tulane.edu',
  }),
}))

const sessionExpiredListeners: Array<() => void> = []
vi.mock('../services/httpClient', () => ({
  onSessionExpired: vi.fn((listener: () => void) => {
    sessionExpiredListeners.push(listener)
    return () => undefined
  }),
}))

vi.mock('../services/profileService', () => ({
  getMyProfileOrNull: vi.fn().mockResolvedValue({ id: 1 }),
}))

function StudentProbe() {
  const { student } = useAuth()
  return <p>student: {student ? student.studentName : 'none'}</p>
}

function Probe() {
  const { hasProfile, markProfileMissing } = useAuth()
  return (
    <>
      <p>hasProfile: {String(hasProfile)}</p>
      <button type="button" onClick={markProfileMissing}>
        Mark missing
      </button>
    </>
  )
}

describe('AuthProvider', () => {
  it('markProfileMissing flips hasProfile to false', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )
    expect(await screen.findByText('hasProfile: true')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Mark missing' }))

    expect(screen.getByText('hasProfile: false')).toBeInTheDocument()
  })

  it('logs the student out when the session ends (for example after a suspension)', async () => {
    render(
      <AuthProvider>
        <StudentProbe />
      </AuthProvider>
    )
    expect(await screen.findByText('student: Jane Doe')).toBeInTheDocument()
    expect(onSessionExpired).toHaveBeenCalled()

    act(() => sessionExpiredListeners.forEach((listener) => listener()))

    expect(screen.getByText('student: none')).toBeInTheDocument()
  })
})
