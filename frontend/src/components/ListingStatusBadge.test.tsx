import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ListingStatusBadge } from './ListingStatusBadge'

describe('ListingStatusBadge', () => {
  it.each([undefined, null, 'ACTIVE'] as const)(
    'renders nothing for %s',
    (status) => {
      const { container } = render(<ListingStatusBadge status={status} />)
      expect(container).toBeEmptyDOMElement()
    }
  )

  it('shows Pending', () => {
    render(<ListingStatusBadge status="PENDING" />)
    expect(screen.getByText('Pending')).toBeInTheDocument()
  })

  it('shows Sold', () => {
    render(<ListingStatusBadge status="SOLD" />)
    expect(screen.getByText('Sold')).toBeInTheDocument()
  })
})
