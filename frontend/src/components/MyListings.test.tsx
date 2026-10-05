import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MyListings } from './MyListings'
import { ApiError } from '../services/httpClient'
import { getListings, type Listing } from '../services/listingService'

vi.mock('../services/listingService', () => ({
  getListings: vi.fn(),
}))

function listing(overrides: Partial<Listing>): Listing {
  return {
    id: 1,
    itemName: 'Desk Lamp',
    description: 'Works great',
    price: 10,
    condition: 'GOOD',
    category: 'FURNITURE',
    sellerId: 7,
    sellerName: 'Jane Doe',
    createdAt: '2026-09-22T00:00:00Z',
    ...overrides,
  }
}

function renderMyListings() {
  return render(
    <MemoryRouter>
      <MyListings sellerId={7} />
    </MemoryRouter>,
  )
}

describe('MyListings', () => {
  beforeEach(() => {
    vi.mocked(getListings).mockReset()
  })

  it('shows a loading state while listings are being fetched', () => {
    vi.mocked(getListings).mockReturnValue(new Promise(() => {}))

    renderMyListings()

    expect(screen.getByText('Loading your listings…')).toBeInTheDocument()
  })

  it('shows only the listings posted by this seller', async () => {
    vi.mocked(getListings).mockResolvedValue([
      listing({ id: 1, itemName: 'Desk Lamp', sellerId: 7 }),
      listing({ id: 2, itemName: 'Mini Fridge', sellerId: 99, sellerName: 'Someone Else' }),
      listing({ id: 3, itemName: 'Office Chair', sellerId: 7 }),
    ])

    renderMyListings()

    expect(await screen.findByText('Desk Lamp')).toBeInTheDocument()
    expect(screen.getByText('Office Chair')).toBeInTheDocument()
    expect(screen.queryByText('Mini Fridge')).not.toBeInTheDocument()
  })

  it('shows how many listings the seller has', async () => {
    vi.mocked(getListings).mockResolvedValue([
      listing({ id: 1, sellerId: 7 }),
      listing({ id: 2, sellerId: 99 }),
      listing({ id: 3, sellerId: 7 }),
    ])

    renderMyListings()

    expect(await screen.findByRole('heading', { name: 'My listings (2)' })).toBeInTheDocument()
  })

  it('keeps the feed order (newest first)', async () => {
    vi.mocked(getListings).mockResolvedValue([
      listing({ id: 3, itemName: 'Newest', sellerId: 7 }),
      listing({ id: 1, itemName: 'Oldest', sellerId: 7 }),
    ])

    renderMyListings()

    await screen.findByText('Newest')
    const titles = screen.getAllByRole('article').map((card) => card.querySelector('h2')?.textContent)
    expect(titles).toEqual(['Newest', 'Oldest'])
  })

  it('shows an empty state linking to the marketplace when the seller has no listings', async () => {
    vi.mocked(getListings).mockResolvedValue([listing({ id: 2, sellerId: 99 })])

    renderMyListings()

    expect(await screen.findByText("You haven't posted any listings yet.")).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Post one in the marketplace' })).toHaveAttribute('href', '/feed')
  })

  it('does not offer edit or delete on profile listings', async () => {
    vi.mocked(getListings).mockResolvedValue([listing({ id: 1, sellerId: 7 })])

    renderMyListings()

    await screen.findByText('Desk Lamp')
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('shows the API error message when listings fail to load', async () => {
    vi.mocked(getListings).mockRejectedValue(new ApiError(500, 'Database unavailable'))

    renderMyListings()

    expect(await screen.findByRole('status')).toHaveTextContent('Database unavailable')
  })

  it('shows a generic message when the request fails without an API error', async () => {
    vi.mocked(getListings).mockRejectedValue(new TypeError('Failed to fetch'))

    renderMyListings()

    expect(await screen.findByRole('status')).toHaveTextContent('Could not load your listings.')
  })
})
