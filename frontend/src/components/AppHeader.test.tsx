import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppHeader } from './AppHeader'

const setStudent = vi.fn()

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent,
  }),
}))

vi.mock('../services/authService', () => ({
  logout: vi.fn().mockResolvedValue(undefined),
}))

function renderHeader() {
  return render(
    <MemoryRouter initialEntries={['/feed']}>
      <Routes>
        <Route path="/feed" element={<AppHeader />} />
        <Route path="/login" element={<p>Login screen</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('AppHeader', () => {
  beforeEach(() => {
    setStudent.mockClear()
  })

  it('shows who is logged in', () => {
    renderHeader()

    expect(screen.getByText('Logged in as Jane Doe')).toBeInTheDocument()
  })

  it('links to the marketplace and the profile page', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'Marketplace' })).toHaveAttribute('href', '/feed')
    expect(screen.getByRole('link', { name: 'My profile' })).toHaveAttribute('href', '/profile')
  })

  it('logs out, clears the student, and goes to the login page', async () => {
    const user = userEvent.setup()
    const { logout } = await import('../services/authService')
    renderHeader()

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(logout).toHaveBeenCalledTimes(1)
    expect(setStudent).toHaveBeenCalledWith(null)
    expect(await screen.findByText('Login screen')).toBeInTheDocument()
  })
})
