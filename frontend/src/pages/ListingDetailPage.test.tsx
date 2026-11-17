import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingDetailPage } from './ListingDetailPage'
import { ApiError } from '../services/httpClient'
import {
  deleteListing,
  getListing,
  updateListingStatus,
  type Listing,
} from '../services/listingService'
import { startConversation } from '../services/conversationService'
import { reportListing } from '../services/reportService'

const authState = vi.hoisted(() => ({ role: 'STUDENT' as 'STUDENT' | 'ADMIN' }))
vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: {
      id: 1,
      studentName: 'Jane Doe',
      email: 'jane@tulane.edu',
      role: authState.role,
    },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/listingService', () => ({
  getListing: vi.fn(),
  deleteListing: vi.fn(),
  updateListingStatus: vi.fn(),
}))

vi.mock('../services/conversationService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/conversationService')>()),
  getConversations: vi.fn().mockResolvedValue([]),
  startConversation: vi.fn(),
}))
vi.mock('../services/reportService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/reportService')>()),
  reportListing: vi.fn(),
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
        <Route path="/conversations/:id" element={<p>Chat screen</p>} />
        <Route path="/feed" element={<p>Feed screen</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ListingDetailPage', () => {
  beforeEach(() => {
    authState.role = 'STUDENT'
    vi.mocked(getListing).mockReset()
    vi.mocked(startConversation).mockReset()
    vi.mocked(deleteListing).mockReset()
    vi.mocked(reportListing).mockReset()
    vi.mocked(updateListingStatus).mockReset()
  })

  it('shows Report listing to another student but hides admin delete', async () => {
    vi.mocked(getListing).mockResolvedValue(listing)
    renderPage()
    expect(
      await screen.findByRole('button', { name: 'Report listing' })
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Delete listing (admin)' })
    ).not.toBeInTheDocument()
  })

  it('lets an admin confirm deletion and returns to feed', async () => {
    authState.role = 'ADMIN'
    vi.mocked(getListing).mockResolvedValue(listing)
    vi.mocked(deleteListing).mockResolvedValue(undefined)
    renderPage()
    expect(
      await screen.findByRole('button', { name: 'Delete listing (admin)' })
    ).toBeInTheDocument()
    // Admins act directly; reporting would only send the listing to their own queue.
    expect(
      screen.queryByRole('button', { name: 'Report listing' })
    ).not.toBeInTheDocument()
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: 'Delete listing (admin)' }))
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: 'Delete' }))
    expect(deleteListing).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Feed screen')).toBeInTheDocument()
  })

  it('hides both actions from the owner', async () => {
    authState.role = 'ADMIN'
    vi.mocked(getListing).mockResolvedValue({ ...listing, sellerId: 1 })
    renderPage()
    await screen.findByRole('heading', { name: 'Desk Lamp' })
    expect(
      screen.queryByRole('button', { name: 'Report listing' })
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Delete listing (admin)' })
    ).not.toBeInTheDocument()
  })

  it('shows the report success message after sending', async () => {
    vi.mocked(getListing).mockResolvedValue(listing)
    vi.mocked(reportListing).mockResolvedValue({
      id: 8,
      listingId: 1,
      reason: 'SPAM',
      details: null,
      status: 'OPEN',
      createdAt: '',
    })
    renderPage()
    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Report listing' }))
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Spam' }))
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: 'Send report' }))
    expect(
      await screen.findByText('Thanks — an admin will review this listing.')
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Report listing' })
    ).not.toBeInTheDocument()
  })

  it('cancels admin deletion without calling the API', async () => {
    authState.role = 'ADMIN'
    vi.mocked(getListing).mockResolvedValue(listing)
    renderPage()
    await userEvent
      .setup()
      .click(
        await screen.findByRole('button', { name: 'Delete listing (admin)' })
      )
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: 'Cancel' }))
    expect(deleteListing).not.toHaveBeenCalled()
  })

  it('renders the listing details', async () => {
    vi.mocked(getListing).mockResolvedValue(listing)
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'Desk Lamp' })
    ).toBeInTheDocument()
    expect(screen.getByText('$10.00')).toBeInTheDocument()
    expect(screen.getByText('Works great')).toBeInTheDocument()
    expect(screen.getByText(/Sold by Jane Doe/)).toBeInTheDocument()
    expect(getListing).toHaveBeenCalledWith(1)
  })

  it('shows not found when the API returns 404', async () => {
    vi.mocked(getListing).mockRejectedValue(
      new ApiError(404, 'Listing not found')
    )
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'Listing not found' })
    ).toBeInTheDocument()
  })

  it('shows not found for a non-numeric id without calling the API', async () => {
    renderPage('/listings/abc')

    expect(
      await screen.findByRole('heading', { name: 'Listing not found' })
    ).toBeInTheDocument()
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

    expect(
      screen.getByRole('link', { name: 'Back to marketplace' })
    ).toHaveAttribute('href', '/feed')
  })

  it('shows the first photo by default', async () => {
    vi.mocked(getListing).mockResolvedValue({
      ...listing,
      imageUrls: ['https://a', 'https://b'],
    })
    renderPage()
    expect(
      await screen.findByRole('img', { name: 'Desk Lamp' })
    ).toHaveAttribute('src', 'https://a')
    expect(
      screen.getByRole('button', { name: 'Show photo 1' })
    ).toHaveAttribute('aria-pressed', 'true')
  })

  it('switches the main photo when a thumbnail is clicked', async () => {
    vi.mocked(getListing).mockResolvedValue({
      ...listing,
      imageUrls: ['https://a', 'https://b'],
    })
    renderPage()
    await screen.findByRole('img', { name: 'Desk Lamp' })
    await userEvent
      .setup()
      .click(screen.getByRole('button', { name: 'Show photo 2' }))
    expect(screen.getByRole('img', { name: 'Desk Lamp' })).toHaveAttribute(
      'src',
      'https://b'
    )
    expect(
      screen.getByRole('button', { name: 'Show photo 2' })
    ).toHaveAttribute('aria-pressed', 'true')
  })

  it('hides the thumbnail row with a single photo', async () => {
    vi.mocked(getListing).mockResolvedValue({
      ...listing,
      imageUrls: ['https://a'],
    })
    renderPage()
    expect(
      await screen.findByRole('img', { name: 'Desk Lamp' })
    ).toHaveAttribute('src', 'https://a')
    expect(
      screen.queryByRole('button', { name: 'Show photo 1' })
    ).not.toBeInTheDocument()
  })

  it('shows the category placeholder with no photos', async () => {
    vi.mocked(getListing).mockResolvedValue({
      ...listing,
      imageUrls: [],
      imageUrl: null,
    })
    renderPage()
    expect(
      await screen.findByRole('img', { name: 'Furniture listing' })
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Show photo/ })
    ).not.toBeInTheDocument()
  })

  describe('Message seller button', () => {
    it('shows Message seller on another student’s listing', async () => {
      vi.mocked(getListing).mockResolvedValue(listing)
      renderPage()
      expect(
        await screen.findByRole('button', { name: 'Message seller' })
      ).toBeEnabled()
    })

    it('shows Message seller on an ACTIVE listing', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, status: 'ACTIVE' })
      renderPage()
      expect(
        await screen.findByRole('button', { name: 'Message seller' })
      ).toBeInTheDocument()
    })

    it('hides Message seller on the student’s own listing', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, sellerId: 1 })
      renderPage()
      await screen.findByRole('heading', { name: 'Desk Lamp' })
      expect(
        screen.queryByRole('button', { name: 'Message seller' })
      ).not.toBeInTheDocument()
    })

    it('hides Message seller and says so when the listing is SOLD', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, status: 'SOLD' })
      renderPage()
      expect(
        await screen.findByText('This item has been sold.')
      ).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Message seller' })
      ).not.toBeInTheDocument()
      expect(screen.getByText('Sold')).toBeInTheDocument()
    })

    it('still shows Message seller on a PENDING listing, with a note', async () => {
      vi.mocked(getListing).mockResolvedValue({ ...listing, status: 'PENDING' })
      renderPage()
      expect(
        await screen.findByRole('button', { name: 'Message seller' })
      ).toBeEnabled()
      expect(screen.getByText('Pending')).toBeInTheDocument()
      expect(
        screen.getByText(/someone is already arranging to buy this/i)
      ).toBeInTheDocument()
    })

    it('hides Message seller while loading and on errors', async () => {
      vi.mocked(getListing).mockRejectedValue(new ApiError(500, 'Server down'))
      renderPage()
      expect(
        screen.queryByRole('button', { name: 'Message seller' })
      ).not.toBeInTheDocument()
      await screen.findByText('Server down')
      expect(
        screen.queryByRole('button', { name: 'Message seller' })
      ).not.toBeInTheDocument()
    })

    it('opens the message popup titled with the item name, without starting a conversation', async () => {
      vi.mocked(getListing).mockResolvedValue(listing)
      renderPage()

      await userEvent
        .setup()
        .click(await screen.findByRole('button', { name: 'Message seller' }))

      expect(
        await screen.findByRole('dialog', { name: 'Desk Lamp' })
      ).toBeInTheDocument()
      expect(startConversation).not.toHaveBeenCalled()
      expect(screen.queryByText('Chat screen')).not.toBeInTheDocument()
    })

    it('closes the popup', async () => {
      vi.mocked(getListing).mockResolvedValue(listing)
      renderPage()

      await userEvent
        .setup()
        .click(await screen.findByRole('button', { name: 'Message seller' }))
      await screen.findByRole('dialog', { name: 'Desk Lamp' })

      await userEvent
        .setup()
        .click(screen.getByRole('button', { name: 'Close' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  describe('Listing status control', () => {
    it('shows the status control to the owner only', async () => {
      vi.mocked(getListing).mockResolvedValue({
        ...listing,
        sellerId: 1,
        status: 'PENDING',
      })
      const view = renderPage()
      expect(
        await screen.findByRole('radiogroup', { name: 'Listing status' })
      ).toBeInTheDocument()
      expect(screen.getByRole('radio', { name: 'Pending' })).toBeChecked()
      view.unmount()

      vi.mocked(getListing).mockResolvedValue({ ...listing, status: 'PENDING' })
      renderPage()
      await screen.findByRole('heading', { name: 'Desk Lamp' })
      expect(
        screen.queryByRole('radiogroup', { name: 'Listing status' })
      ).not.toBeInTheDocument()
    })

    it('updates the badge after the owner marks it sold, without reloading', async () => {
      const user = userEvent.setup()
      const own = { ...listing, sellerId: 1, status: 'ACTIVE' as const }
      vi.mocked(getListing).mockResolvedValue(own)
      vi.mocked(updateListingStatus).mockResolvedValue({
        ...own,
        status: 'SOLD',
      })
      renderPage()
      await user.click(await screen.findByRole('radio', { name: 'Sold' }))
      expect(updateListingStatus).toHaveBeenCalledWith(1, 'SOLD')
      await waitFor(() =>
        expect(screen.getByRole('radio', { name: 'Sold' })).toBeChecked()
      )
      // One "Sold" is the radio option's label; the other is the new badge.
      expect(screen.getAllByText('Sold')).toHaveLength(2)
      expect(getListing).toHaveBeenCalledTimes(1)
      expect(
        screen.queryByText('This item has been sold.')
      ).not.toBeInTheDocument()
    })
  })
})
