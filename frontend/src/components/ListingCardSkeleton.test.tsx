import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ListingGridSkeleton } from './ListingCardSkeleton'

describe('ListingGridSkeleton', () => {
  it('renders eight skeleton cards by default', () => {
    render(<ListingGridSkeleton />)

    expect(screen.getAllByTestId('listing-card-skeleton')).toHaveLength(8)
    expect(screen.getByRole('status')).toHaveTextContent('Loading listings')
  })

  it('respects count', () => {
    render(<ListingGridSkeleton count={3} />)

    expect(screen.getAllByTestId('listing-card-skeleton')).toHaveLength(3)
  })
})
