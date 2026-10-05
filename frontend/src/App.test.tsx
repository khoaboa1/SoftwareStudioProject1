import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { getCurrentStudent } from './services/authService'
import { ApiError, getMyProfile, getMyProfileOrNull } from './services/profileService'

vi.mock('./services/authService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./services/authService')>()),
  getCurrentStudent: vi.fn(),
}))

vi.mock('./services/profileService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./services/profileService')>()),
  getMyProfile: vi.fn(),
  getMyProfileOrNull: vi.fn(),
}))

const student = {
  id: 1,
  studentName: 'Jane Doe',
  email: 'jane@tulane.edu',
  emailVerified: true,
  sellingItems: null,
  dormLocation: null,
}

const profile = {
  id: 1,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: null,
  schoolDomain: 'tulane.edu',
  studentId: 1,
  createdAt: '2026-09-22T00:00:00',
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('/profile route', () => {
  beforeEach(() => {
    vi.mocked(getCurrentStudent).mockReset()
    vi.mocked(getMyProfileOrNull).mockReset()
    vi.mocked(getMyProfile).mockReset()
  })

  it('shows the profile page to a logged-in student with a profile', async () => {
    vi.mocked(getCurrentStudent).mockResolvedValue(student)
    vi.mocked(getMyProfileOrNull).mockResolvedValue(profile)
    vi.mocked(getMyProfile).mockResolvedValue(profile)

    renderAt('/profile')

    expect(await screen.findByRole('heading', { level: 1, name: 'My profile' })).toBeInTheDocument()
    expect(await screen.findByText('Computer Science')).toBeInTheDocument()
  })

  it('lands on login when the profile request returns 401', async () => {
    vi.mocked(getCurrentStudent).mockResolvedValue(student)
    vi.mocked(getMyProfileOrNull).mockResolvedValue(profile)
    vi.mocked(getMyProfile).mockRejectedValue(new ApiError(401, 'Session expired'))

    renderAt('/profile')

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
  })

  it('lands on profile setup when the profile request returns 404', async () => {
    vi.mocked(getCurrentStudent).mockResolvedValue(student)
    vi.mocked(getMyProfileOrNull).mockResolvedValue(profile)
    vi.mocked(getMyProfile).mockRejectedValue(new ApiError(404, 'Profile not found'))

    renderAt('/profile')

    expect(await screen.findByRole('button', { name: 'Continue to marketplace' })).toBeInTheDocument()
  })

  it('sends a logged-out visitor to the login page', async () => {
    vi.mocked(getCurrentStudent).mockResolvedValue(null)

    renderAt('/profile')

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: 'My profile' })).not.toBeInTheDocument()
  })

  it('sends a student without a profile to profile setup', async () => {
    vi.mocked(getCurrentStudent).mockResolvedValue(student)
    vi.mocked(getMyProfileOrNull).mockResolvedValue(null)

    renderAt('/profile')

    expect(await screen.findByRole('button', { name: 'Continue to marketplace' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: 'My profile' })).not.toBeInTheDocument()
  })
})
