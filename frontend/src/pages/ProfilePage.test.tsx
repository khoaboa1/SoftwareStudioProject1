import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ProfilePage } from './ProfilePage'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent: vi.fn(),
  }),
}))

describe('ProfilePage', () => {
  it('renders the app header and the profile', () => {
    render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'My profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'My profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument()
    expect(screen.getByText(/Member since/)).toBeInTheDocument()
  })
})
