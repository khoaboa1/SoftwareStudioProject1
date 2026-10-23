import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FeedPage } from './FeedPage'
import { ApiError } from '../services/httpClient'
import { getListings, searchListings, type Listing, type ListingPage } from '../services/listingService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/listingService', () => ({
  getListings: vi.fn(),
  searchListings: vi.fn(),
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

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname + location.search}</span>
}

function renderFeed(path = '/feed') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <LocationProbe />
      <Routes><Route path="/feed" element={<FeedPage />} /></Routes>
    </MemoryRouter>,
  )
}

function page(items: Listing[]): ListingPage {
  return { items, meta: { totalItems: items.length, currentPage: 1, totalPages: 1, pageSize: 100 } }
}

describe('FeedPage', () => {
  beforeEach(() => {
    vi.mocked(getListings).mockReset()
    vi.mocked(searchListings).mockReset()
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

  it('loads the search results when the URL has q', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([listing(1, 'Desk Lamp')]))
    renderFeed('/feed?q=lamp')

    expect(await screen.findByText('Desk Lamp')).toBeInTheDocument()
    expect(searchListings).toHaveBeenCalledWith({ q: 'lamp', sortBy: 'createdAt', sortOrder: 'desc', limit: 100 })
    expect(getListings).not.toHaveBeenCalled()
    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('lamp')
  })

  it('uses the normal feed for a whitespace-only URL query', async () => {
    vi.mocked(getListings).mockResolvedValue([listing(1, 'Desk Lamp')])
    renderFeed('/feed?q=%20%20%20')

    expect(await screen.findByText('Desk Lamp')).toBeInTheDocument()
    expect(getListings).toHaveBeenCalled()
    expect(searchListings).not.toHaveBeenCalled()
  })

  it('typing a keyword updates the URL and searches', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    vi.mocked(searchListings).mockResolvedValue(page([]))
    renderFeed()
    await userEvent.setup().type(screen.getByRole('searchbox', { name: 'Search listings' }), 'chair')

    expect(await screen.findByText('/feed?q=chair', { selector: 'span' }, { timeout: 1000 })).toBeInTheDocument()
    expect(searchListings).toHaveBeenCalledWith({ q: 'chair', sortBy: 'createdAt', sortOrder: 'desc', limit: 100 })
  })

  it('keeps other query params when searching', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    vi.mocked(searchListings).mockResolvedValue(page([]))
    renderFeed('/feed?category=DECOR')
    await userEvent.setup().type(screen.getByRole('searchbox', { name: 'Search listings' }), 'lamp{Enter}')

    expect(screen.getByTestId('location')).toHaveTextContent('/feed?category=DECOR&q=lamp')
  })

  it('clearing the search returns to the full feed', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([listing(1, 'Desk Lamp')]))
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed('/feed?q=lamp')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Clear search' }))

    expect(screen.getByTestId('location')).toHaveTextContent('/feed')
    expect(getListings).toHaveBeenCalled()
  })

  it('Clear Filters empties the search box', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([]))
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed('/feed?q=zzz')
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Clear Filters' }))

    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('')
    expect(screen.getByTestId('location')).toHaveTextContent('/feed')
  })

  it('ignores a stale search response', async () => {
    let resolveFirst!: (value: ListingPage) => void
    vi.mocked(searchListings)
      .mockReturnValueOnce(new Promise((resolve) => { resolveFirst = resolve }))
      .mockResolvedValueOnce(page([listing(2, 'Bookshelf')]))
    renderFeed('/feed?q=a')
    await userEvent.setup().type(screen.getByRole('searchbox', { name: 'Search listings' }), 'b{Enter}')

    expect(await screen.findByText('Bookshelf')).toBeInTheDocument()
    await act(async () => { resolveFirst(page([listing(1, 'Old Result')])) })
    expect(screen.queryByText('Old Result')).not.toBeInTheDocument()
  })

  it('restores every control from the URL', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([]))
    renderFeed('/feed?q=desk&category=furniture&minPrice=10&maxPrice=50&sort=price_asc')

    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('desk')
    expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('furniture')
    expect(screen.getByRole('spinbutton', { name: 'Min price' })).toHaveValue(10)
    expect(screen.getByRole('spinbutton', { name: 'Max price' })).toHaveValue(50)
    expect(screen.getByRole('combobox', { name: 'Sort by' })).toHaveValue('price_asc')
    await waitFor(() => expect(searchListings).toHaveBeenCalledWith({
      q: 'desk', category: 'FURNITURE', minPrice: 10, maxPrice: 50,
      sortBy: 'price', sortOrder: 'asc', limit: 100,
    }))
    expect(getListings).not.toHaveBeenCalled()
  })

  it('sorting keeps the keyword and category and reorders immediately', async () => {
    const first = [listing(1, 'A'), listing(2, 'B')]
    first[0].price = 30
    vi.mocked(searchListings).mockResolvedValueOnce(page(first)).mockResolvedValueOnce(page([first[1], first[0]]))
    renderFeed('/feed?q=lamp&category=decor')
    expect(await screen.findByRole('link', { name: 'A' })).toBeInTheDocument()

    await userEvent.setup().selectOptions(screen.getByRole('combobox', { name: 'Sort by' }), 'price_asc')
    expect(screen.getByTestId('location')).toHaveTextContent('q=lamp')
    expect(screen.getByTestId('location')).toHaveTextContent('category=decor')
    expect(screen.getByTestId('location')).toHaveTextContent('sort=price_asc')
    await waitFor(() => expect(searchListings).toHaveBeenLastCalledWith({
      q: 'lamp', category: 'DECOR', sortBy: 'price', sortOrder: 'asc', limit: 100,
    }))
    await waitFor(() => expect(screen.getAllByRole('link', { name: /^[AB]$/ }).map((link) => link.textContent)).toEqual(['B', 'A']))
  })

  it('choosing a category alone uses the search API', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    vi.mocked(searchListings).mockResolvedValue(page([]))
    renderFeed()
    await userEvent.setup().selectOptions(screen.getByRole('combobox', { name: 'Category' }), 'furniture')
    expect(screen.getByTestId('location')).toHaveTextContent('/feed?category=furniture')
    await waitFor(() => expect(searchListings).toHaveBeenCalledWith({
      category: 'FURNITURE', sortBy: 'createdAt', sortOrder: 'desc', limit: 100,
    }))
  })

  it('ignores garbage params', async () => {
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed('/feed?category=banana&minPrice=abc&sort=xyz')
    await waitFor(() => expect(getListings).toHaveBeenCalled())
    expect(searchListings).not.toHaveBeenCalled()
    expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('')
  })

  it('Clear filters resets everything including the search box', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([]))
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed('/feed?q=desk&category=furniture&sort=price_desc')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Clear filters' }))
    expect(screen.getByTestId('location')).toHaveTextContent('/feed')
    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('')
    expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('')
    expect(screen.getByRole('combobox', { name: 'Sort by' })).toHaveValue('newest')
  })

  it('Clear filters cancels pending keyword and price changes', async () => {
    vi.mocked(searchListings).mockResolvedValue(page([]))
    vi.mocked(getListings).mockResolvedValue([])
    renderFeed('/feed?category=decor')
    await screen.findByText('No listings match your filters')
    vi.useFakeTimers()
    try {
      fireEvent.change(screen.getByRole('searchbox', { name: 'Search listings' }), { target: { value: 'lamp' } })
      fireEvent.change(screen.getByRole('spinbutton', { name: 'Min price' }), { target: { value: '15' } })
      fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
      act(() => vi.advanceTimersByTime(400))
      expect(screen.getByTestId('location')).toHaveTextContent('/feed')
      expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('')
      expect(screen.getByRole('spinbutton', { name: 'Min price' })).toHaveValue(null)
    } finally {
      vi.useRealTimers()
    }
  })
})
