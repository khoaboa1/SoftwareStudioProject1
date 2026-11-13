import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { ConversationRow } from './ConversationRow'
import type { ConversationSummary } from '../services/conversationService'

const base: ConversationSummary = {
  id: 5, listingId: 2, itemName: 'Desk Lamp', otherPartyId: 7, otherPartyName: 'Sam Seller',
  activityAt: '2026-11-13T10:00:00Z', sold: false, lastMessage: { sender: 'You', text: 'Can I pick it up today?' },
}

function renderRow(overrides: Partial<ConversationSummary> = {}) {
  return render(<MemoryRouter><ConversationRow conversation={{ ...base, ...overrides }} /></MemoryRouter>)
}

describe('ConversationRow', () => {
  it('links to the chat and shows the person, the item subtitle and the last message', () => {
    renderRow()

    const link = screen.getByRole('link', { name: 'Conversation with Sam Seller about Desk Lamp' })
    expect(link).toHaveAttribute('href', '/conversations/5')
    expect(screen.getByRole('heading', { name: 'Sam Seller' })).toBeInTheDocument()
    expect(screen.getByText('Desk Lamp')).toBeInTheDocument()
    expect(link).toHaveTextContent('You: Can I pick it up today?')
  })

  it('shows who sent the last message when it was the other person', () => {
    renderRow({ lastMessage: { sender: 'Sam', text: 'Yes, still available' } })
    expect(screen.getByRole('link')).toHaveTextContent('Sam: Yes, still available')
  })

  it('shows a placeholder when there are no messages yet', () => {
    renderRow({ lastMessage: null })
    expect(screen.getByText('No messages yet')).toBeInTheDocument()
  })

  it('shows the Sold badge only for sold listings', () => {
    const { unmount } = renderRow({ sold: true })
    expect(screen.getByText('Sold')).toBeInTheDocument()
    unmount()
    renderRow()
    expect(screen.queryByText('Sold')).not.toBeInTheDocument()
  })

  it('uses the last activity time', () => {
    renderRow()
    expect(screen.getByRole('link').querySelector('time')).toHaveAttribute('dateTime', '2026-11-13T10:00:00Z')
  })
})
