import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingDetailPage } from './ListingDetailPage'
import { ApiError } from '../services/httpClient'
import { getListing, type Listing } from '../services/listingService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/listingService', () => ({
  getListing: vi.fn(),
}))

const listing: Listing = {
  id: 1,
  itemName: 'Desk Lamp',
  description: 'Works great',
  price: 10,
  condition: 'GOOD',
  category: 'FURNITURE',
  sellerId: 7,
  sellerName: 'Jane Doe',
  createdAt: '2026-09-22T00:00:00Z',
}

function renderPage(path = '/listings/1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/listings/:id" element={<ListingDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ListingDetailPage', () => {
  beforeEach(() => {
    vi.mocked(getListing).mockReset()
  })

  it('renders the listing details', async () => {
    vi.mocked(getListing).mockResolvedValue(listing)
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Desk Lamp' })).toBeInTheDocument()
    expect(screen.getByText('$10.00')).toBeInTheDocument()
    expect(screen.getByText('Works great')).toBeInTheDocument()
    expect(screen.getByText(/Sold by Jane Doe/)).toBeInTheDocument()
    expect(getListing).toHaveBeenCalledWith(1)
  })

  it('shows not found when the API returns 404', async () => {
    vi.mocked(getListing).mockRejectedValue(new ApiError(404, 'Listing not found'))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Listing not found' })).toBeInTheDocument()
  })

  it('shows not found for a non-numeric id without calling the API', async () => {
    renderPage('/listings/abc')

    expect(await screen.findByRole('heading', { name: 'Listing not found' })).toBeInTheDocument()
    expect(getListing).not.toHaveBeenCalled()
  })

  it('shows an error message for other failures', async () => {
    vi.mocked(getListing).mockRejectedValue(new ApiError(500, 'Server down'))
    renderPage()

    expect(await screen.findByText('Server down')).toBeInTheDocument()
  })

  it('links back to the marketplace', () => {
    vi.mocked(getListing).mockReturnValue(new Promise(() => {}))
    renderPage()

    expect(screen.getByRole('link', { name: 'Back to marketplace' })).toHaveAttribute('href', '/feed')
  })
})
