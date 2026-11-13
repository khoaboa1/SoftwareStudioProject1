import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ConversationPage } from './ConversationPage'
import { ApiError } from '../services/httpClient'
import {
  getConversation,
  getMessages,
  sendMessage,
  type Conversation,
  type Message,
} from '../services/conversationService'

vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ student: { id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu' }, setStudent: vi.fn() }),
}))

vi.mock('../services/conversationService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/conversationService')>()),
  getConversations: vi.fn().mockResolvedValue([]),
  getConversation: vi.fn(),
  getMessages: vi.fn(),
  sendMessage: vi.fn(),
}))

const POLL_MS = 5000

const conversation: Conversation = {
  id: 5, listingId: 2, itemName: 'Desk Lamp',
  buyerId: 1, buyerName: 'Jane Doe', sellerId: 7, sellerName: 'Sam Seller',
  createdAt: '2026-11-10T10:00:00Z', listingStatus: 'ACTIVE',
}

function message(id: number, senderId: number, content: string): Message {
  return {
    id, conversationId: 5, senderId,
    senderName: senderId === 1 ? 'Jane Doe' : 'Sam Seller',
    content, createdAt: `2026-11-13T10:0${id % 10}:00Z`,
  }
}

function renderChat(path = '/conversations/5') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/conversations/:id" element={<><Link to="/conversations/6">Open chat 6</Link><ConversationPage /></>} />
        <Route path="/login" element={<p>Login screen</p>} />
        <Route path="/profile-setup" element={<p>Setup screen</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

function messageList() {
  return screen.getByRole('list', { name: 'Messages' })
}

describe('ConversationPage', () => {
  let scrollIntoView: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.mocked(getConversation).mockReset().mockResolvedValue(conversation)
    vi.mocked(getMessages).mockReset().mockResolvedValue([])
    vi.mocked(sendMessage).mockReset()
    scrollIntoView = vi.fn()
    Element.prototype.scrollIntoView = scrollIntoView
  })

  afterEach(() => {
    vi.useRealTimers()
    Reflect.deleteProperty(document, 'visibilityState')
  })

  describe('loading and header', () => {
    it('shows a loading state', () => {
      vi.mocked(getConversation).mockReturnValue(new Promise(() => {}))
      renderChat()
      expect(screen.getByRole('status')).toHaveTextContent('Loading conversation')
    })

    it('loads the conversation and messages for the id in the URL', async () => {
      renderChat()
      await screen.findByRole('heading', { name: 'Sam Seller' })

      expect(getConversation).toHaveBeenCalledWith(5)
      expect(getMessages).toHaveBeenCalledWith(5)
    })

    it('shows the other person, a link to the item and a link back to all messages', async () => {
      renderChat()

      expect(await screen.findByRole('heading', { name: 'Sam Seller' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Desk Lamp' })).toHaveAttribute('href', '/listings/2')
      expect(screen.getByRole('link', { name: /All messages/ })).toHaveAttribute('href', '/inbox')
      expect(screen.queryByText('Sold')).not.toBeInTheDocument()
    })

    it('shows the buyer as the other person when I am the seller', async () => {
      vi.mocked(getConversation).mockResolvedValue({ ...conversation, buyerId: 9, buyerName: 'Bob Buyer', sellerId: 1, sellerName: 'Jane Doe' })
      renderChat()
      expect(await screen.findByRole('heading', { name: 'Bob Buyer' })).toBeInTheDocument()
    })

    it('shows a Sold badge on a sold listing and still allows messaging', async () => {
      vi.mocked(getConversation).mockResolvedValue({ ...conversation, listingStatus: 'SOLD' })
      renderChat()

      expect(await screen.findByText('Sold')).toBeInTheDocument()
      expect(screen.getByRole('textbox', { name: 'Message' })).toBeEnabled()
    })
  })

  describe('messages', () => {
    it('shows my messages and theirs on the correct side, oldest first', async () => {
      vi.mocked(getMessages).mockResolvedValue([message(1, 7, 'Hi, still interested?'), message(2, 1, 'Yes!')])
      renderChat()

      await screen.findByText('Yes!')
      const items = within(messageList()).getAllByRole('listitem')
      expect(items).toHaveLength(2)
      expect(items[0]).toHaveTextContent('Hi, still interested?')
      expect(items[0]).toHaveAttribute('data-mine', 'false')
      expect(items[1]).toHaveAttribute('data-mine', 'true')
    })

    it('scrolls to the newest message after loading', async () => {
      vi.mocked(getMessages).mockResolvedValue([message(1, 7, 'Hi')])
      renderChat()

      await screen.findByText('Hi')
      expect(scrollIntoView).toHaveBeenCalled()
    })

    it('shows an empty state when there are no messages', async () => {
      renderChat()
      expect(await screen.findByText('No messages yet. Say hi about Desk Lamp.')).toBeInTheDocument()
    })
  })

  describe('sending', () => {
    it('shows my message immediately, then the saved copy', async () => {
      let resolveSend: (m: Message) => void = () => {}
      vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
      const user = userEvent.setup()
      renderChat()

      await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Can I pick it up today?{Enter}')

      expect(sendMessage).toHaveBeenCalledWith(5, 'Can I pick it up today?')
      const pending = within(messageList()).getByText('Can I pick it up today?').closest('li')
      expect(pending).toHaveAttribute('data-mine', 'true')
      expect(pending).toHaveAttribute('aria-busy', 'true')

      await act(async () => { resolveSend(message(3, 1, 'Can I pick it up today?')) })
      const saved = within(messageList()).getByText('Can I pick it up today?').closest('li')
      expect(saved).not.toHaveAttribute('aria-busy', 'true')
      expect(within(messageList()).getAllByRole('listitem')).toHaveLength(1)
    })

    it('marks a failed message as Not sent and retries it', async () => {
      vi.mocked(sendMessage)
        .mockRejectedValueOnce(new ApiError(500, 'Server down'))
        .mockResolvedValueOnce(message(3, 1, 'Hello'))
      const user = userEvent.setup()
      renderChat()

      await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Hello{Enter}')
      expect(await screen.findByText(/Not sent/)).toBeInTheDocument()

      await user.click(within(messageList()).getByRole('button', { name: 'Retry' }))
      await waitFor(() => expect(screen.queryByText(/Not sent/)).not.toBeInTheDocument())
      expect(sendMessage).toHaveBeenCalledTimes(2)
      expect(sendMessage).toHaveBeenLastCalledWith(5, 'Hello')
      expect(within(messageList()).getAllByRole('listitem')).toHaveLength(1)
    })

    it('shows an inline error when the backend rejects the content', async () => {
      vi.mocked(sendMessage).mockRejectedValue(new ApiError(400, 'Message cannot be empty'))
      const user = userEvent.setup()
      renderChat()

      await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'x{Enter}')
      expect(await screen.findByText('Message cannot be empty')).toBeInTheDocument()
    })
  })

  describe('polling every 5 seconds', () => {
    beforeEach(() => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
    })

    it('fetches only messages after the last one and appends them', async () => {
      vi.mocked(getMessages)
        .mockResolvedValueOnce([message(1, 7, 'Hi')])
        .mockResolvedValueOnce([message(2, 7, 'Are you there?')])
      renderChat()
      await screen.findByText('Hi')

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })

      expect(getMessages).toHaveBeenLastCalledWith(5, 1)
      expect(await screen.findByText('Are you there?')).toBeInTheDocument()
    })

    it('does not poll before 5 seconds have passed', async () => {
      renderChat()
      await screen.findByRole('heading', { name: 'Sam Seller' })

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS - 100) })
      expect(getMessages).toHaveBeenCalledTimes(1)
    })

    it('does not duplicate a message it already has', async () => {
      vi.mocked(getMessages).mockResolvedValue([message(1, 7, 'Hi')])
      renderChat()
      await screen.findByText('Hi')

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
      expect(within(messageList()).getAllByRole('listitem')).toHaveLength(1)
    })

    it('pauses while the tab is hidden', async () => {
      renderChat()
      await screen.findByRole('heading', { name: 'Sam Seller' })
      Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'hidden' })

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS * 3) })
      expect(getMessages).toHaveBeenCalledTimes(1)
    })

    it('stops polling after leaving the page', async () => {
      const { unmount } = renderChat()
      await screen.findByRole('heading', { name: 'Sam Seller' })
      unmount()

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS * 3) })
      expect(getMessages).toHaveBeenCalledTimes(1)
    })
  })

  describe('errors', () => {
    it('redirects to login on 401', async () => {
      vi.mocked(getConversation).mockRejectedValue(new ApiError(401, 'Not logged in'))
      renderChat()
      expect(await screen.findByText('Login screen')).toBeInTheDocument()
    })

    it('redirects to profile setup when the profile is missing', async () => {
      vi.mocked(getConversation).mockRejectedValue(new ApiError(404, 'Profile not found'))
      renderChat()
      expect(await screen.findByText('Setup screen')).toBeInTheDocument()
    })

    it.each([
      [403, 'You are not part of this conversation'],
      [404, 'Conversation not found'],
    ])('shows Conversation not found on %i', async (status, apiMessage) => {
      vi.mocked(getConversation).mockRejectedValue(new ApiError(status, apiMessage))
      renderChat()

      expect(await screen.findByRole('heading', { name: 'Conversation not found' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /All messages/ })).toHaveAttribute('href', '/inbox')
    })

    it('shows Conversation not found for a non-numeric id without calling the API', async () => {
      renderChat('/conversations/abc')

      expect(await screen.findByRole('heading', { name: 'Conversation not found' })).toBeInTheDocument()
      expect(getConversation).not.toHaveBeenCalled()
    })

    it('shows other errors with a Retry that reloads', async () => {
      vi.mocked(getConversation).mockRejectedValueOnce(new ApiError(500, 'Server down')).mockResolvedValueOnce(conversation)
      const user = userEvent.setup()
      renderChat()

      expect(await screen.findByText('Server down')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Retry' }))
      expect(await screen.findByRole('heading', { name: 'Sam Seller' })).toBeInTheDocument()
    })
  })

  describe('regressions', () => {
    it('does not request messages for a non-numeric id', async () => {
      renderChat('/conversations/abc')
      await screen.findByRole('heading', { name: 'Conversation not found' })
      expect(getMessages).not.toHaveBeenCalled()
    })

    it('does not load messages or poll after a Conversation not found error', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      vi.mocked(getConversation).mockRejectedValue(new ApiError(404, 'Conversation not found'))
      renderChat()
      await screen.findByRole('heading', { name: 'Conversation not found' })

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS * 3) })
      expect(getMessages).not.toHaveBeenCalled()
    })

    it('scrolls the newest message into view inside the list', async () => {
      vi.mocked(getMessages).mockResolvedValue([message(1, 7, 'First'), message(2, 7, 'Latest')])
      renderChat()
      await screen.findByText('Latest')

      const latest = within(messageList()).getAllByRole('listitem')[1]
      expect(scrollIntoView.mock.contexts).toContain(latest)
    })

    it('keeps the typed text and shows the reason when the backend rejects a message', async () => {
      vi.mocked(sendMessage).mockRejectedValue(new ApiError(400, 'Message cannot be empty'))
      const user = userEvent.setup()
      renderChat()

      const box = await screen.findByRole('textbox', { name: 'Message' })
      await user.type(box, 'draft text{Enter}')

      expect(await screen.findByText('Message cannot be empty')).toBeInTheDocument()
      expect(box).toHaveValue('draft text')
      expect(within(messageList()).queryAllByRole('listitem')).toHaveLength(0)
    })

    it('does not wipe the next message being typed when the previous send finishes', async () => {
      let resolveSend: (m: Message) => void = () => {}
      vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
      const user = userEvent.setup()
      renderChat()

      const box = await screen.findByRole('textbox', { name: 'Message' })
      await user.type(box, 'one{Enter}')
      await user.type(box, 'two')
      await act(async () => { resolveSend(message(3, 1, 'one')) })

      expect(box).toHaveValue('two')
    })

    it('keeps focus in the text box while a message is sending', async () => {
      vi.mocked(sendMessage).mockReturnValue(new Promise(() => {}))
      const user = userEvent.setup()
      renderChat()

      const box = await screen.findByRole('textbox', { name: 'Message' })
      await user.type(box, 'hi{Enter}')
      expect(box).toHaveFocus()
    })

    describe('while a message is in flight', () => {
      beforeEach(() => { vi.useFakeTimers({ shouldAdvanceTime: true }) })

      it('polls with the last real message id, ignoring the unsent one', async () => {
        vi.mocked(getMessages).mockResolvedValue([message(1, 7, 'Hi')])
        vi.mocked(sendMessage).mockReturnValue(new Promise(() => {}))
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
        renderChat()

        await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'pending{Enter}')
        await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })

        expect(getMessages).toHaveBeenLastCalledWith(5, 1)
      })

      it('shows one copy when polling delivers the message before the send returns', async () => {
        const mine = message(3, 1, 'Mine')
        vi.mocked(getMessages).mockResolvedValueOnce([]).mockResolvedValue([mine])
        let resolveSend: (m: Message) => void = () => {}
        vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
        renderChat()

        await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Mine{Enter}')
        await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
        await act(async () => { resolveSend(mine) })

        expect(within(messageList()).getAllByRole('listitem')).toHaveLength(1)
      })

      it('keeps my message before a reply that arrived while it was sending', async () => {
        vi.mocked(getMessages).mockResolvedValueOnce([]).mockResolvedValue([message(4, 7, 'Their reply')])
        let resolveSend: (m: Message) => void = () => {}
        vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
        renderChat()

        await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'Mine{Enter}')
        await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
        await act(async () => { resolveSend(message(3, 1, 'Mine')) })

        const items = within(messageList()).getAllByRole('listitem')
        expect(items.map((item) => item.textContent)).toEqual([
          expect.stringContaining('Mine'),
          expect.stringContaining('Their reply'),
        ])
      })
    })

    it('does not throw on a failed poll and keeps polling', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      vi.mocked(getMessages).mockResolvedValueOnce([message(1, 7, 'Hi')]).mockRejectedValueOnce(new ApiError(500, 'down')).mockResolvedValue([message(2, 7, 'Back')])
      renderChat()
      await screen.findByText('Hi')

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
      expect(await screen.findByText('Back')).toBeInTheDocument()
    })

    it('redirects to login when the session expires during a poll', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      vi.mocked(getMessages).mockResolvedValueOnce([]).mockRejectedValue(new ApiError(401, 'Not logged in'))
      renderChat()
      await screen.findByRole('heading', { name: 'Sam Seller' })

      await act(async () => { await vi.advanceTimersByTimeAsync(POLL_MS) })
      expect(await screen.findByText('Login screen')).toBeInTheDocument()
    })

    it('does not show "No messages yet" next to a messages error', async () => {
      vi.mocked(getMessages).mockRejectedValue(new ApiError(500, 'Messages down'))
      renderChat()

      expect(await screen.findByText('Messages down')).toBeInTheDocument()
      expect(screen.queryByText(/No messages yet/)).not.toBeInTheDocument()
    })

    it('drops a pending send when another conversation is opened', async () => {
      let resolveSend: (m: Message) => void = () => {}
      vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
      vi.mocked(getMessages).mockImplementation(async (id) => (id === 6 ? [message(9, 7, 'Chat six')] : []))
      const user = userEvent.setup()
      renderChat()

      await user.type(await screen.findByRole('textbox', { name: 'Message' }), 'For chat five{Enter}')
      await user.click(screen.getByRole('link', { name: 'Open chat 6' }))
      expect(await screen.findByText('Chat six')).toBeInTheDocument()
      await act(async () => { resolveSend(message(3, 1, 'For chat five')) })

      expect(screen.queryByText('For chat five')).not.toBeInTheDocument()
      expect(within(messageList()).getAllByRole('listitem')).toHaveLength(1)
    })

    it('clears the old chat while another conversation loads', async () => {
      vi.mocked(getMessages).mockImplementation(async (id) => (id === 6 ? new Promise(() => {}) : [message(1, 7, 'Chat five')]))
      const user = userEvent.setup()
      renderChat()

      await screen.findByText('Chat five')
      await user.click(screen.getByRole('link', { name: 'Open chat 6' }))
      await screen.findByRole('heading', { name: 'Sam Seller' })

      expect(screen.queryByText('Chat five')).not.toBeInTheDocument()
    })
  })
})
