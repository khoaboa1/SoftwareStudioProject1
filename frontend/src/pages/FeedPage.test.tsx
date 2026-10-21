import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FeedPage } from './FeedPage'
import { ApiError } from '../services/httpClient'
import { getListings, type Listing } from '../services/listingService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/listingService', () => ({
  getListings: vi.fn(),
  getCategories: vi.fn().mockResolvedValue([]),
  createListing: vi.fn(),
  updateListing: vi.fn(),
  deleteListing: vi.fn(),
}))

function listing(id: number, itemName: string): Listing {
  return {
    id,
    itemName,
    description: 'Works great',
    price: 10,
    condition: 'GOOD',
    category: 'FURNITURE',
    sellerId: 1,
    sellerName: 'Jane Doe',
    createdAt: '2026-09-22T00:00:00Z',
  }
}

function renderFeed() {
  return render(<MemoryRouter initialEntries={['/feed']}><FeedPage /></MemoryRouter>)
}

describe('FeedPage', () => {
  beforeEach(() => {
    vi.mocked(getListings).mockReset()
  })

  it('renders the shared app header with navigation', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed()

    expect(screen.getByText('Logged in as Jane Doe')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My profile' })).toHaveAttribute('href', '/profile')
    expect(await screen.findByText('No listings yet. Be the first to post one!')).toBeInTheDocument()
  })

  it('shows skeletons while the feed is loading', () => {
    vi.mocked(getListings).mockReturnValue(new Promise(() => {}))
    renderFeed()

    expect(screen.getByRole('status')).toHaveTextContent('Loading listings')
    expect(screen.queryByText('No listings yet. Be the first to post one!')).not.toBeInTheDocument()
  })

  it('shows the empty state when there are no listings', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed()

    expect(await screen.findByText('No listings yet. Be the first to post one!')).toBeInTheDocument()
  })

  it('renders a card per listing', async () => {
    vi.mocked(getListings).mockResolvedValue([listing(1, 'Desk Lamp'), listing(2, 'Office Chair')])
    renderFeed()

    expect(await screen.findByText('Desk Lamp')).toBeInTheDocument()
    expect(screen.getByText('Office Chair')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Desk Lamp' })).toHaveAttribute('href', '/listings/1')
    expect(screen.getByRole('link', { name: 'Office Chair' })).toHaveAttribute('href', '/listings/2')
  })

  it('shows an error instead of the empty state when the feed fails', async () => {
    vi.mocked(getListings).mockRejectedValue(new ApiError(500, 'Server down'))
    renderFeed()

    expect(await screen.findByText('Server down')).toBeInTheDocument()
    expect(screen.queryByText('No listings yet. Be the first to post one!')).not.toBeInTheDocument()
  })
})
