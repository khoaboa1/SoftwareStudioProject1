import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProfilePage } from './ProfilePage'
import { ApiError, getMyProfile, type Profile } from '../services/profileService'

const setStudent = vi.fn()
const markProfileMissing = vi.fn()

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent,
    markProfileMissing,
  }),
}))

vi.mock('../services/profileService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/profileService')>()),
  getMyProfile: vi.fn(),
}))

const profile: Profile = {
  id: 1,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: 'Junior studying CS and Math.',
  schoolDomain: 'tulane.edu',
  studentId: 1,
  createdAt: '2026-09-22T00:00:00',
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <Routes>
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/login" element={<p>Login screen</p>} />
        <Route path="/profile-setup" element={<p>Profile setup screen</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getMyProfile).mockReset()
    setStudent.mockClear()
    markProfileMissing.mockClear()
  })

  it('shows a loading state while the profile is being fetched', () => {
    vi.mocked(getMyProfile).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByRole('heading', { level: 1, name: 'My profile' })).toBeInTheDocument()
    expect(screen.getByLabelText('Loading profile')).toBeInTheDocument()
  })

  it('renders the profile returned by the API', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(profile)

    renderPage()

    expect(await screen.findByRole('heading', { level: 2, name: 'Jane Doe' })).toBeInTheDocument()
    expect(screen.getByText('tulane.edu')).toBeInTheDocument()
    expect(screen.getByText('Junior studying CS and Math.')).toBeInTheDocument()
    expect(getMyProfile).toHaveBeenCalled()
  })

  it('clears the session and goes to login on 401', async () => {
    vi.mocked(getMyProfile).mockRejectedValue(new ApiError(401, 'User must be authenticated'))

    renderPage()

    expect(await screen.findByText('Login screen')).toBeInTheDocument()
    expect(setStudent).toHaveBeenCalledWith(null)
  })

  it('marks the profile missing and goes to profile setup on 404', async () => {
    vi.mocked(getMyProfile).mockRejectedValue(new ApiError(404, 'Profile not found'))

    renderPage()

    expect(await screen.findByText('Profile setup screen')).toBeInTheDocument()
    expect(markProfileMissing).toHaveBeenCalledTimes(1)
  })

  it('shows the API error message for other failures', async () => {
    vi.mocked(getMyProfile).mockRejectedValue(new ApiError(500, 'Database unavailable'))

    renderPage()

    expect(await screen.findByRole('status')).toHaveTextContent('Database unavailable')
  })

  it('shows a generic message when the request fails without an API error', async () => {
    vi.mocked(getMyProfile).mockRejectedValue(new TypeError('Failed to fetch'))

    renderPage()

    expect(await screen.findByRole('status')).toHaveTextContent('Could not load your profile.')
  })
})
