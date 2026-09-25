import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ListingCard } from './ListingCard'
import type { Listing } from '../services/listingService'

const listing: Listing = {
  id: 1,
  itemName: 'Desk Lamp',
  description: 'Works great',
  price: 10,
  condition: 'GOOD',
  category: 'FURNITURE',
  sellerId: 7,
  sellerName: 'Jane Doe',
  createdAt: '2026-01-01T00:00:00Z',
}

describe('ListingCard', () => {
  it('renders the listing details', () => {
    render(<ListingCard listing={listing} />)

    expect(screen.getByText('Desk Lamp')).toBeInTheDocument()
    expect(screen.getByText('Works great')).toBeInTheDocument()
    expect(screen.getByText('$10.00')).toBeInTheDocument()
    expect(screen.getByText(/Jane Doe/)).toBeInTheDocument()
  })

  it('does not render a delete button when onDelete is not provided', () => {
    render(<ListingCard listing={listing} />)

    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('calls onDelete when the delete button is clicked', async () => {
    const user = userEvent.setup()
    const handleDelete = vi.fn()

    render(<ListingCard listing={listing} onDelete={handleDelete} />)
    await user.click(screen.getByRole('button', { name: 'Delete' }))

    expect(handleDelete).toHaveBeenCalledTimes(1)
  })
})
