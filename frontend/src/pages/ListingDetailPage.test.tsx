import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

  it('shows the first photo by default', async () => {
    vi.mocked(getListing).mockResolvedValue({ ...listing, imageUrls: ['https://a', 'https://b'] })
    renderPage()
    expect(await screen.findByRole('img', { name: 'Desk Lamp' })).toHaveAttribute('src', 'https://a')
    expect(screen.getByRole('button', { name: 'Show photo 1' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('switches the main photo when a thumbnail is clicked', async () => {
    vi.mocked(getListing).mockResolvedValue({ ...listing, imageUrls: ['https://a', 'https://b'] })
    renderPage()
    await screen.findByRole('img', { name: 'Desk Lamp' })
    await userEvent.setup().click(screen.getByRole('button', { name: 'Show photo 2' }))
    expect(screen.getByRole('img', { name: 'Desk Lamp' })).toHaveAttribute('src', 'https://b')
    expect(screen.getByRole('button', { name: 'Show photo 2' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('hides the thumbnail row with a single photo', async () => {
    vi.mocked(getListing).mockResolvedValue({ ...listing, imageUrls: ['https://a'] })
    renderPage()
    expect(await screen.findByRole('img', { name: 'Desk Lamp' })).toHaveAttribute('src', 'https://a')
    expect(screen.queryByRole('button', { name: 'Show photo 1' })).not.toBeInTheDocument()
  })

  it('shows the category placeholder with no photos', async () => {
    vi.mocked(getListing).mockResolvedValue({ ...listing, imageUrls: [], imageUrl: null })
    renderPage()
    expect(await screen.findByRole('img', { name: 'Furniture listing' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Show photo/ })).not.toBeInTheDocument()
  })

  describe('Message seller button', () => {
    it('shows Message seller on another student’s listing', async () => {
      vi.mocked(getListing).mockResolvedValue(listing)
      renderPage()
      expect(await screen.findByRole('button', { name: 'Message seller' })).toBeEnabled()
    })

    it('shows Message seller on an ACTIVE listing', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, status: 'ACTIVE' })
      renderPage()
      expect(await screen.findByRole('button', { name: 'Message seller' })).toBeInTheDocument()
    })

    it('hides Message seller on the student’s own listing', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, sellerId: 1 })
      renderPage()
      await screen.findByRole('heading', { name: 'Desk Lamp' })
      expect(screen.queryByRole('button', { name: 'Message seller' })).not.toBeInTheDocument()
    })

    it.each(['INACTIVE', 'SOLD'])('hides Message seller when the listing is %s', async (status) => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, status })
      renderPage()
      await screen.findByRole('heading', { name: 'Desk Lamp' })
      expect(screen.queryByRole('button', { name: 'Message seller' })).not.toBeInTheDocument()
    })

    it('hides Message seller while loading and on errors', async () => {
      vi.mocked(getListing).mockRejectedValue(new ApiError(500, 'Server down'))
      renderPage()
      expect(screen.queryByRole('button', { name: 'Message seller' })).not.toBeInTheDocument()
      await screen.findByText('Server down')
      expect(screen.queryByRole('button', { name: 'Message seller' })).not.toBeInTheDocument()
    })
  })
})
