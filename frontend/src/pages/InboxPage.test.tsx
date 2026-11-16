import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { InboxPage } from './InboxPage'
import { ApiError } from '../services/httpClient'
import { getConversations, type Conversation } from '../services/conversationService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' }, setStudent: vi.fn() }),
}))

vi.mock('../services/conversationService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/conversationService')>()),
  getConversations: vi.fn(),
}))

function conversation(id: number, createdAt: string, overrides: Partial<Conversation> = {}): Conversation {
  return {
    id, listingId: id, itemName: `Item ${id}`, buyerId: 1, buyerName: 'Jane Doe',
    sellerId: 2, sellerName: `Seller ${id}`, createdAt, ...overrides,
  }
}

function renderInbox() {
  return render(
    <MemoryRouter initialEntries={['/inbox']}>
      <Routes>
        <Route path="/inbox" element={<InboxPage />} />
        <Route path="/login" element={<p>Login screen</p>} />
        <Route path="/profile-setup" element={<p>Setup screen</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('InboxPage', () => {
  beforeEach(() => { vi.mocked(getConversations).mockReset() })

  it('shows a loading state', () => {
    vi.mocked(getConversations).mockReturnValue(new Promise(() => {}))
    renderInbox()
    expect(screen.getByText('Loading conversations')).toBeInTheDocument()
  })

  it('lists conversations newest first with the other party and a sold badge', async () => {
    vi.mocked(getConversations).mockResolvedValue([
      conversation(1, '2026-11-01T00:00:00Z'),
      conversation(2, '2026-11-05T00:00:00Z', { listingStatus: 'SOLD' }),
    ])
    renderInbox()

    const rows = await screen.findAllByRole('link', { name: /Conversation with/ })
    expect(rows[0]).toHaveAccessibleName('Conversation with Seller 2 about Item 2')
    expect(rows[0]).toHaveAttribute('href', '/conversations/2')
    expect(rows[0]).toHaveTextContent('Sold')
    expect(rows[1]).not.toHaveTextContent('Sold')
  })

  it('shows the empty state', async () => {
    vi.mocked(getConversations).mockResolvedValue([])
    renderInbox()
    expect(await screen.findByText('No conversations yet')).toBeInTheDocument()
  })

  it('shows an error with a retry that refetches', async () => {
    vi.mocked(getConversations).mockRejectedValueOnce(new ApiError(500, 'Server down')).mockResolvedValueOnce([])
    renderInbox()
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Retry' }))

    expect(await screen.findByText('No conversations yet')).toBeInTheDocument()
    expect(getConversations).toHaveBeenCalledTimes(2)
  })

  it('redirects to login on 401', async () => {
    vi.mocked(getConversations).mockRejectedValue(new ApiError(401, 'Nope'))
    renderInbox()
    expect(await screen.findByText('Login screen')).toBeInTheDocument()
  })

  it('redirects to profile setup on 404', async () => {
    vi.mocked(getConversations).mockRejectedValue(new ApiError(404, 'Profile not found'))
    renderInbox()
    expect(await screen.findByText('Setup screen')).toBeInTheDocument()
  })
})
