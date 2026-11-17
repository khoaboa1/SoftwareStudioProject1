import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../services/httpClient'
import type { Listing } from '../services/listingService'
import type { Conversation, Message } from '../services/conversationService'
import {
  getConversations,
  getMessages,
  sendMessage,
  startConversation,
} from '../services/conversationService'
import { MessageSellerModal } from './MessageSellerModal'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: {
      id: 1,
      studentName: 'Bea Buyer',
      email: 'bea@tulane.edu',
      role: 'STUDENT',
    },
    setStudent: vi.fn(),
  }),
}))

vi.mock('../services/conversationService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/conversationService')>()),
  getConversations: vi.fn(),
  getMessages: vi.fn(),
  startConversation: vi.fn(),
  sendMessage: vi.fn(),
}))

const listing: Listing = {
  id: 5,
  itemName: 'Desk Lamp',
  description: 'Works great',
  price: 25,
  condition: 'GOOD',
  category: 'FURNITURE',
  sellerId: 7,
  sellerName: 'Sam Seller',
  createdAt: '2026-11-01T00:00:00Z',
}

function conversation(id: number, listingId: number): Conversation {
  return {
    id,
    listingId,
    itemName: 'Desk Lamp',
    buyerId: 1,
    buyerName: 'Bea Buyer',
    sellerId: 7,
    sellerName: 'Sam Seller',
    createdAt: '2026-11-10T10:00:00Z',
  }
}

function message(id: number, senderId: number, content: string): Message {
  return {
    id,
    conversationId: 42,
    senderId,
    senderName: senderId === 1 ? 'Bea Buyer' : 'Sam Seller',
    content,
    createdAt: '2026-11-10T10:05:00Z',
  }
}

function renderModal(onClose = vi.fn()) {
  render(
    <MemoryRouter initialEntries={['/feed']}>
      <Routes>
        <Route
          path="/feed"
          element={<MessageSellerModal listing={listing} onClose={onClose} />}
        />
        <Route path="/login" element={<p>Login screen</p>} />
      </Routes>
    </MemoryRouter>
  )
  return onClose
}

describe('MessageSellerModal', () => {
  beforeEach(() => {
    vi.mocked(getConversations).mockReset()
    vi.mocked(getMessages).mockReset()
    vi.mocked(startConversation).mockReset()
    vi.mocked(sendMessage).mockReset()
    vi.mocked(getConversations).mockResolvedValue([])
  })

  it('shows the item name as the dialog title and the seller below it', async () => {
    renderModal()
    await screen.findByRole('dialog', { name: 'Desk Lamp' })
    expect(screen.getByText('Sold by Sam Seller · $25.00')).toBeInTheDocument()
  })

  it('shows the empty prompt without creating a conversation', async () => {
    renderModal()
    await screen.findByText('No messages yet. Say hi about Desk Lamp.')
    expect(startConversation).not.toHaveBeenCalled()
    expect(getMessages).not.toHaveBeenCalled()
  })

  it('loads the history of an existing conversation for this item', async () => {
    vi.mocked(getConversations).mockResolvedValue([
      conversation(43, 99),
      conversation(42, 5),
    ])
    vi.mocked(getMessages).mockResolvedValue([
      message(1, 7, 'Is it available?'),
      message(2, 1, 'Yes, I want it'),
    ])
    renderModal()
    await screen.findByText('Is it available?')
    expect(screen.getByText('Yes, I want it')).toBeInTheDocument()
    expect(getMessages).toHaveBeenCalledWith(42)
    expect(getMessages).toHaveBeenCalledTimes(1)
    expect(
      screen.getByRole('link', { name: 'Open full conversation' })
    ).toHaveAttribute('href', '/conversations/42')
    expect(
      screen.queryByText('No messages yet. Say hi about Desk Lamp.')
    ).not.toBeInTheDocument()
  })

  it('creates the conversation on the first send and sends the message', async () => {
    vi.mocked(startConversation).mockResolvedValue(conversation(42, 5))
    vi.mocked(sendMessage).mockResolvedValue(message(100, 1, 'Hi'))
    const user = userEvent.setup()
    renderModal()
    await user.type(
      await screen.findByRole('textbox', { name: 'Message' }),
      'Hi'
    )
    await user.click(screen.getByRole('button', { name: 'Send' }))
    await screen.findByText('Hi')
    expect(startConversation).toHaveBeenCalledWith(5)
    expect(sendMessage).toHaveBeenCalledWith(42, 'Hi')
    expect(
      screen.getByRole('link', { name: 'Open full conversation' })
    ).toHaveAttribute('href', '/conversations/42')
    expect(
      screen.queryByText('No messages yet. Say hi about Desk Lamp.')
    ).not.toBeInTheDocument()
  })

  it('reuses the conversation on the next send', async () => {
    vi.mocked(startConversation).mockResolvedValue(conversation(42, 5))
    vi.mocked(sendMessage)
      .mockResolvedValueOnce(message(100, 1, 'Hi'))
      .mockResolvedValue(message(101, 1, 'Still there?'))
    const user = userEvent.setup()
    renderModal()
    await user.type(
      await screen.findByRole('textbox', { name: 'Message' }),
      'Hi'
    )
    await user.click(screen.getByRole('button', { name: 'Send' }))
    await screen.findByText('Hi')
    await user.type(
      await screen.findByRole('textbox', { name: 'Message' }),
      'Still there?'
    )
    await user.click(screen.getByRole('button', { name: 'Send' }))
    expect(startConversation).toHaveBeenCalledTimes(1)
    expect(sendMessage).toHaveBeenLastCalledWith(42, 'Still there?')
  })

  it.each([
    [409, 'This item has already been sold'],
    [403, 'You cannot message yourself about your own listing'],
  ])(
    'shows the backend message and keeps the text on %i',
    async (status: number, apiMessage: string) => {
      vi.mocked(startConversation).mockRejectedValue(
        new ApiError(status, apiMessage)
      )
      const user = userEvent.setup()
      renderModal()
      await user.type(
        await screen.findByRole('textbox', { name: 'Message' }),
        'Hi'
      )
      await user.click(screen.getByRole('button', { name: 'Send' }))
      await screen.findByText(apiMessage)
      expect(screen.getByRole('textbox', { name: 'Message' })).toHaveValue('Hi')
      expect(sendMessage).not.toHaveBeenCalled()
    }
  )

  it('shows a load error but still lets the user send', async () => {
    vi.mocked(getConversations).mockRejectedValue(
      new ApiError(500, 'Server down')
    )
    renderModal()
    await screen.findByText('Server down')
    expect(screen.getByRole('textbox', { name: 'Message' })).toBeEnabled()
  })

  it('shows a sent message even after the history failed to load', async () => {
    vi.mocked(getConversations).mockRejectedValue(
      new ApiError(500, 'Server down')
    )
    vi.mocked(startConversation).mockResolvedValue(conversation(42, 5))
    vi.mocked(sendMessage).mockResolvedValue(message(100, 1, 'Hi'))
    const user = userEvent.setup()
    renderModal()
    await screen.findByText('Server down')
    await user.type(screen.getByRole('textbox', { name: 'Message' }), 'Hi')
    await user.click(screen.getByRole('button', { name: 'Send' }))
    expect(await screen.findByText('Hi')).toBeInTheDocument()
    expect(screen.queryByText('Server down')).not.toBeInTheDocument()
  })

  it('sends the user to login on 401', async () => {
    vi.mocked(getConversations).mockRejectedValue(
      new ApiError(401, 'Unauthorized')
    )
    renderModal()
    await screen.findByText('Login screen')
  })

  it('closes on the Close button, on Escape and on a backdrop click, not on a click inside', async () => {
    const user = userEvent.setup()
    const onClose = renderModal()
    await user.click(screen.getByRole('button', { name: 'Close' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(2)
    await user.click(screen.getByRole('dialog'))
    expect(onClose).toHaveBeenCalledTimes(2)
    await user.click(screen.getByTestId('message-modal-backdrop'))
    expect(onClose).toHaveBeenCalledTimes(3)
  })
})
