import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProfilePage } from './ProfilePage'
import { ApiError, getMyProfile, updateProfile, type Profile } from '../services/profileService'

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
  updateProfile: vi.fn(),
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
    vi.mocked(updateProfile).mockReset()
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

  describe('editing', () => {
    async function openEditor() {
      const user = userEvent.setup()
      vi.mocked(getMyProfile).mockResolvedValue(profile)
      renderPage()
      await user.click(await screen.findByRole('button', { name: 'Edit profile' }))
      return user
    }

    it('opens the edit form prefilled with the profile', async () => {
      await openEditor()

      expect(screen.getByLabelText('Name')).toHaveValue('Jane Doe')
      expect(screen.queryByRole('button', { name: 'Edit profile' })).not.toBeInTheDocument()
    })

    it('returns to the unchanged profile on cancel', async () => {
      const user = await openEditor()

      await user.clear(screen.getByLabelText('Major'))
      await user.type(screen.getByLabelText('Major'), 'Physics')
      await user.click(screen.getByRole('button', { name: 'Cancel' }))

      expect(screen.getByText('Computer Science')).toBeInTheDocument()
      expect(screen.queryByText('Physics')).not.toBeInTheDocument()
      expect(updateProfile).not.toHaveBeenCalled()
    })

    it('saves the changes and shows the updated profile', async () => {
      vi.mocked(updateProfile).mockResolvedValue({ ...profile, major: 'Data Science', bio: 'Senior now.' })
      const user = await openEditor()

      await user.clear(screen.getByLabelText('Major'))
      await user.type(screen.getByLabelText('Major'), 'Data Science')
      await user.clear(screen.getByLabelText('Bio'))
      await user.type(screen.getByLabelText('Bio'), 'Senior now.')
      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(updateProfile).toHaveBeenCalledWith(1, { name: 'Jane Doe', major: 'Data Science', bio: 'Senior now.' })
      expect(await screen.findByText('Profile updated.')).toBeInTheDocument()
      expect(screen.getByText('Data Science')).toBeInTheDocument()
      expect(screen.getByText('Senior now.')).toBeInTheDocument()
      expect(screen.queryByLabelText('Name')).not.toBeInTheDocument()
    })

    it('keeps the form open and shows the message on 403', async () => {
      vi.mocked(updateProfile).mockRejectedValue(
        new ApiError(403, 'You do not have permission to modify this profile.'),
      )
      const user = await openEditor()

      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByRole('status')).toHaveTextContent('You do not have permission to modify this profile.')
      expect(screen.getByLabelText('Name')).toBeInTheDocument()
    })

    it('keeps the form open and shows the message on 400', async () => {
      vi.mocked(updateProfile).mockRejectedValue(new ApiError(400, 'bio: Bio cannot exceed 1000 characters'))
      const user = await openEditor()

      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByRole('status')).toHaveTextContent('bio: Bio cannot exceed 1000 characters')
    })

    it('shows a generic message when saving fails without an API error', async () => {
      vi.mocked(updateProfile).mockRejectedValue(new TypeError('Failed to fetch'))
      const user = await openEditor()

      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByRole('status')).toHaveTextContent('Could not save your profile.')
    })

    it('clears the session and goes to login on 401', async () => {
      vi.mocked(updateProfile).mockRejectedValue(new ApiError(401, 'User must be authenticated'))
      const user = await openEditor()

      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByText('Login screen')).toBeInTheDocument()
      expect(setStudent).toHaveBeenCalledWith(null)
    })

    it('goes to profile setup on 404', async () => {
      vi.mocked(updateProfile).mockRejectedValue(new ApiError(404, 'Profile not found'))
      const user = await openEditor()

      await user.click(screen.getByRole('button', { name: 'Save changes' }))

      expect(await screen.findByText('Profile setup screen')).toBeInTheDocument()
      expect(markProfileMissing).toHaveBeenCalledTimes(1)
    })
  })
})
