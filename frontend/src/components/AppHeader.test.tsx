import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppHeader } from './AppHeader'
import { getOpenReports } from '../services/adminService'

const setStudent = vi.fn()
const authState = vi.hoisted(() => ({
  role: undefined as 'ADMIN' | 'STUDENT' | undefined,
}))

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: {
      id: 1,
      studentName: 'Jane Doe',
      email: 'jane@tulane.edu',
      role: authState.role,
    },
    setStudent,
  }),
}))

vi.mock('../services/adminService', () => ({
  getOpenReports: vi.fn().mockResolvedValue([]),
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
    </MemoryRouter>
  )
}

describe('AppHeader', () => {
  beforeEach(() => {
    setStudent.mockClear()
    authState.role = undefined
  })

  it('shows Admin only for an admin', () => {
    const missingRole = renderHeader()
    expect(
      screen.queryByRole('link', { name: 'Admin' })
    ).not.toBeInTheDocument()
    missingRole.unmount()
    authState.role = 'ADMIN'
    const view = renderHeader()
    expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute(
      'href',
      '/admin'
    )
    view.unmount()
    authState.role = 'STUDENT'
    renderHeader()
    expect(
      screen.queryByRole('link', { name: 'Admin' })
    ).not.toBeInTheDocument()
  })

  it('shows who is logged in', () => {
    renderHeader()

    expect(screen.getByText('Logged in as Jane Doe')).toBeInTheDocument()
  })

  it('links to the marketplace and the profile page', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'Marketplace' })).toHaveAttribute(
      'href',
      '/feed'
    )
    expect(screen.getByRole('link', { name: 'My profile' })).toHaveAttribute(
      'href',
      '/profile'
    )
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

describe('AppHeader open-report badge', () => {
  beforeEach(() => {
    authState.role = 'ADMIN'
    vi.mocked(getOpenReports).mockReset()
  })

  it('shows the open report count on the Admin link', async () => {
    vi.mocked(getOpenReports).mockResolvedValue(
      [1, 2, 3].map((id) => ({ id }) as never)
    )
    renderHeader()
    expect(
      await screen.findByRole('link', { name: 'Admin, 3 open reports' })
    ).toHaveAttribute('href', '/admin')
  })

  it('shows no badge when there are no open reports', async () => {
    vi.mocked(getOpenReports).mockResolvedValue([])
    renderHeader()
    expect(
      await screen.findByRole('link', { name: 'Admin' })
    ).toBeInTheDocument()
    expect(getOpenReports).toHaveBeenCalledTimes(1)
  })

  it('never loads reports for a student', () => {
    authState.role = 'STUDENT'
    renderHeader()
    expect(getOpenReports).not.toHaveBeenCalled()
  })

  it('uses a count it is given instead of loading one', () => {
    render(
      <MemoryRouter>
        <AppHeader openReportCount={5} />
      </MemoryRouter>
    )
    expect(
      screen.getByRole('link', { name: 'Admin, 5 open reports' })
    ).toBeInTheDocument()
    expect(getOpenReports).not.toHaveBeenCalled()
  })
})
