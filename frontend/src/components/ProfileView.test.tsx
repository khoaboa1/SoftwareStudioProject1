import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ProfileView } from './ProfileView'
import type { Profile } from '../services/profileService'

const profile: Profile = {
  id: 1,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: 'Junior studying CS and Math.',
  schoolDomain: 'tulane.edu',
  studentId: 1,
  createdAt: '2026-09-22T00:00:00',
}

describe('ProfileView', () => {
  it('renders the profile details', () => {
    render(<ProfileView status="ready" profile={profile} />)

    expect(screen.getByRole('heading', { name: 'Jane Doe' })).toBeInTheDocument()
    expect(screen.getByText('Computer Science')).toBeInTheDocument()
    expect(screen.getByText('tulane.edu')).toBeInTheDocument()
    expect(screen.getByText('Junior studying CS and Math.')).toBeInTheDocument()
  })

  it('shows the initials of the first and last name', () => {
    render(<ProfileView status="ready" profile={{ ...profile, name: 'Jane Q Doe' }} />)

    expect(screen.getByText('JD')).toBeInTheDocument()
  })

  it('shows a single initial for a one-word name', () => {
    render(<ProfileView status="ready" profile={{ ...profile, name: 'Cher' }} />)

    expect(screen.getByText('C')).toBeInTheDocument()
  })

  it('shows the month and year the profile was created', () => {
    render(<ProfileView status="ready" profile={profile} />)

    expect(screen.getByText('Member since Sep 2026')).toBeInTheDocument()
  })

  it('shows a placeholder when the bio is empty', () => {
    render(<ProfileView status="ready" profile={{ ...profile, bio: null }} />)

    expect(screen.getByText('No bio yet.')).toBeInTheDocument()
  })

  it('treats a whitespace-only bio as empty', () => {
    render(<ProfileView status="ready" profile={{ ...profile, bio: '   ' }} />)

    expect(screen.getByText('No bio yet.')).toBeInTheDocument()
  })

  it('shows a loading state without profile details', () => {
    render(<ProfileView status="loading" />)

    expect(screen.getByLabelText('Loading profile')).toBeInTheDocument()
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })

  it('shows the error message', () => {
    render(<ProfileView status="error" error="Could not load your profile." />)

    expect(screen.getByRole('status')).toHaveTextContent('Could not load your profile.')
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })

  it('falls back to a generic error message', () => {
    render(<ProfileView status="error" />)

    expect(screen.getByRole('status')).toHaveTextContent('Something went wrong loading this profile.')
  })
})
