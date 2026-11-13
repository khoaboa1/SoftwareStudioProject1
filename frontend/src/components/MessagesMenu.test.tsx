import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MessagesMenu } from './MessagesMenu'
import { getConversations, type Conversation } from '../services/conversationService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' } }),
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

function renderMenu() {
  return render(<MemoryRouter><MessagesMenu /></MemoryRouter>)
}

describe('MessagesMenu', () => {
  beforeEach(() => { vi.mocked(getConversations).mockReset() })

  it('does not fetch or show the box until the button is pressed', () => {
    renderMenu()
    expect(screen.queryByRole('region', { name: 'Messages' })).not.toBeInTheDocument()
    expect(getConversations).not.toHaveBeenCalled()
  })

  it('shows All messages and only the latest 3 conversations, newest first', async () => {
    vi.mocked(getConversations).mockResolvedValue([
      conversation(1, '2026-11-01T00:00:00Z'),
      conversation(2, '2026-11-04T00:00:00Z'),
      conversation(3, '2026-11-02T00:00:00Z'),
      conversation(4, '2026-11-03T00:00:00Z'),
    ])
    renderMenu()
    await userEvent.setup().click(screen.getByRole('button', { name: /message/i }))

    expect(screen.getByRole('link', { name: 'All messages' })).toHaveAttribute('href', '/inbox')
    const rows = await screen.findAllByRole('link', { name: /Conversation with/ })
    expect(rows.map((row) => row.getAttribute('href'))).toEqual([
      '/conversations/2', '/conversations/4', '/conversations/3',
    ])
  })

  it('resolves the other party for a seller', async () => {
    vi.mocked(getConversations).mockResolvedValue([
      conversation(5, '2026-11-01T00:00:00Z', { buyerId: 9, buyerName: 'Bob Buyer', sellerId: 1, sellerName: 'Jane Doe' }),
    ])
    renderMenu()
    await userEvent.setup().click(screen.getByRole('button', { name: /message/i }))

    expect(await screen.findByRole('link', { name: 'Conversation with Bob Buyer about Item 5' })).toBeInTheDocument()
  })

  it('shows an empty message when there are no conversations', async () => {
    vi.mocked(getConversations).mockResolvedValue([])
    renderMenu()
    await userEvent.setup().click(screen.getByRole('button', { name: /message/i }))

    expect(await screen.findByText('No conversations yet')).toBeInTheDocument()
  })

  it('shows an error and retries', async () => {
    vi.mocked(getConversations).mockRejectedValueOnce(new Error('boom')).mockResolvedValueOnce([])
    renderMenu()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /message/i }))
    await user.click(await screen.findByRole('button', { name: 'Retry' }))

    expect(await screen.findByText('No conversations yet')).toBeInTheDocument()
  })

  it('closes on Escape', async () => {
    vi.mocked(getConversations).mockResolvedValue([])
    renderMenu()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /message/i }))
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('region', { name: 'Messages' })).not.toBeInTheDocument()
  })

  it('shows the last message of each conversation and orders by last activity', async () => {
    vi.mocked(getConversations).mockResolvedValue([
      conversation(1, '2026-11-05T00:00:00Z'),
      conversation(2, '2026-11-01T00:00:00Z', { lastMessage: { content: 'See you at 5', senderId: 1, createdAt: '2026-11-09T00:00:00Z' } }),
    ])
    renderMenu()
    await userEvent.setup().click(screen.getByRole('button', { name: /message/i }))

    const rows = await screen.findAllByRole('link', { name: /Conversation with/ })
    expect(rows.map((row) => row.getAttribute('href'))).toEqual(['/conversations/2', '/conversations/1'])
    expect(rows[0]).toHaveTextContent('You: See you at 5')
    expect(rows[1]).toHaveTextContent('No messages yet')
  })
})
