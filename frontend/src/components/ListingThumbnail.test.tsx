import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ListingThumbnail } from './ListingThumbnail'

describe('ListingThumbnail', () => {
  it('labels the placeholder with the category', () => {
    render(<ListingThumbnail category="ELECTRONICS" />)

    expect(screen.getByRole('img', { name: 'Electronics listing' })).toBeInTheDocument()
  })

  it('renders the photo when imageUrl is set', () => {
    const url = 'https://images.unsplash.com/photo-1?w=800'
    const { container } = render(<ListingThumbnail category="ELECTRONICS" imageUrl={url} itemName="Mini Fridge" />)
    expect(screen.getByRole('img', { name: 'Mini Fridge' })).toHaveAttribute('src', url)
    expect(screen.getByRole('img', { name: 'Mini Fridge' })).toHaveClass('object-cover')
    expect(container.firstChild).toHaveClass('aspect-[4/3]')
  })

  it('keeps the category placeholder when imageUrl is null', () => {
    render(<ListingThumbnail category="ELECTRONICS" imageUrl={null} />)
    expect(screen.getByRole('img', { name: 'Electronics listing' })).toBeInTheDocument()
  })

  it('falls back to the category icon when the image fails to load', () => {
    const url = 'https://images.unsplash.com/photo-1?w=800'
    render(<ListingThumbnail category="ELECTRONICS" imageUrl={url} itemName="Mini Fridge" />)
    fireEvent.error(screen.getByRole('img', { name: 'Mini Fridge' }))
    expect(screen.getByRole('img', { name: 'Electronics listing' })).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: 'Mini Fridge' })).not.toBeInTheDocument()
  })
})
