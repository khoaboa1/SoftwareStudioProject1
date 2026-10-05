import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { FeedPage } from './FeedPage'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/listingService', () => ({
  getListings: vi.fn().mockResolvedValue([]),
  getCategories: vi.fn().mockResolvedValue([]),
  createListing: vi.fn(),
  updateListing: vi.fn(),
  deleteListing: vi.fn(),
}))

describe('FeedPage', () => {
  it('renders the shared app header with navigation', async () => {
    render(
      <MemoryRouter>
        <FeedPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Logged in as Jane Doe')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My profile' })).toHaveAttribute('href', '/profile')
    expect(await screen.findByText('No listings yet. Be the first to post one!')).toBeInTheDocument()
  })
})
