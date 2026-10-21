import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
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

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname}</span>
}

function renderCard(onEdit?: () => void, onDelete?: () => void) {
  return render(
    <MemoryRouter initialEntries={['/feed']}>
      <ListingCard listing={listing} onEdit={onEdit} onDelete={onDelete} />
      <LocationProbe />
    </MemoryRouter>,
  )
}

describe('ListingCard', () => {
  it('renders the listing details', () => {
    renderCard()

    expect(screen.getByText('Desk Lamp')).toBeInTheDocument()
    expect(screen.queryByText('Works great')).not.toBeInTheDocument()
    expect(screen.getByText('$10.00')).toBeInTheDocument()
    expect(screen.getByText('Furniture')).toBeInTheDocument()
    expect(screen.getByText('Good')).toBeInTheDocument()
    expect(screen.getByText(/Jane Doe/)).toBeInTheDocument()
  })

  it('links the card to the listing detail page', () => {
    renderCard()

    expect(screen.getByRole('link', { name: 'Desk Lamp' })).toHaveAttribute('href', '/listings/1')
  })

  it('does not render edit or delete buttons when the handlers are not provided', () => {
    renderCard()

    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('calls onDelete without navigating', async () => {
    const user = userEvent.setup()
    const handleDelete = vi.fn()

    renderCard(undefined, handleDelete)
    await user.click(screen.getByRole('button', { name: 'Delete' }))

    expect(handleDelete).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('location')).toHaveTextContent('/feed')
  })

  it('calls onEdit without navigating', async () => {
    const user = userEvent.setup()
    const handleEdit = vi.fn()

    renderCard(handleEdit)
    await user.click(screen.getByRole('button', { name: 'Edit' }))

    expect(handleEdit).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('location')).toHaveTextContent('/feed')
  })
})
