import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './auth-context'

vi.mock('../services/authService', () => ({
  getCurrentStudent: vi.fn().mockResolvedValue({ id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' }),
}))

vi.mock('../services/profileService', () => ({
  getMyProfileOrNull: vi.fn().mockResolvedValue({ id: 1 }),
}))

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
      </AuthProvider>,
    )
    expect(await screen.findByText('hasProfile: true')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Mark missing' }))

    expect(screen.getByText('hasProfile: false')).toBeInTheDocument()
  })
})
