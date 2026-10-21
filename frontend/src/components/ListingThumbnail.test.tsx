import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ListingThumbnail } from './ListingThumbnail'

describe('ListingThumbnail', () => {
  it('labels the placeholder with the category', () => {
    render(<ListingThumbnail category="ELECTRONICS" />)

    expect(screen.getByRole('img', { name: 'Electronics listing' })).toBeInTheDocument()
  })
})
