import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingStatusControl } from './ListingStatusControl'
import { ApiError } from '../services/httpClient'
import { updateListingStatus, type Listing } from '../services/listingService'

vi.mock('../services/listingService', () => ({
  updateListingStatus: vi.fn(),
}))

const updated = { id: 3, status: 'SOLD' } as Listing

describe('ListingStatusControl', () => {
  beforeEach(() => {
    vi.mocked(updateListingStatus).mockReset()
  })

  it('shows the three statuses with the current one checked', () => {
    render(
      <ListingStatusControl listingId={3} status="PENDING" onChange={vi.fn()} />
    )
    expect(
      screen.getByRole('radiogroup', { name: 'Listing status' })
    ).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Available' })).not.toBeChecked()
    expect(screen.getByRole('radio', { name: 'Pending' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Sold' })).not.toBeChecked()
  })

  it('saves the chosen status and reports the updated listing', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    vi.mocked(updateListingStatus).mockResolvedValue(updated)
    render(
      <ListingStatusControl listingId={3} status="ACTIVE" onChange={onChange} />
    )
    await user.click(screen.getByRole('radio', { name: 'Sold' }))
    expect(updateListingStatus).toHaveBeenCalledWith(3, 'SOLD')
    expect(onChange).toHaveBeenCalledWith(updated)
  })

  it('shows the error and keeps the old status when saving fails', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    vi.mocked(updateListingStatus).mockRejectedValue(
      new ApiError(403, 'You do not have permission to modify this listing')
    )
    render(
      <ListingStatusControl listingId={3} status="ACTIVE" onChange={onChange} />
    )
    await user.click(screen.getByRole('radio', { name: 'Sold' }))
    expect(
      await screen.findByText(
        'You do not have permission to modify this listing'
      )
    ).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Available' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Sold' })).not.toBeChecked()
    expect(onChange).not.toHaveBeenCalled()
  })

  it('disables the options while saving', async () => {
    const user = userEvent.setup()
    let resolve: (listing: Listing) => void = () => {}
    vi.mocked(updateListingStatus).mockReturnValue(
      new Promise((r) => {
        resolve = r
      })
    )
    render(
      <ListingStatusControl listingId={3} status="ACTIVE" onChange={vi.fn()} />
    )
    await user.click(screen.getByRole('radio', { name: 'Pending' }))
    expect(screen.getByRole('radio', { name: 'Sold' })).toBeDisabled()
    resolve(updated)
    expect(
      await screen.findByRole('radio', { name: 'Sold' })
    ).not.toBeDisabled()
  })

  it('does nothing when the current status is chosen again', async () => {
    const user = userEvent.setup()
    render(
      <ListingStatusControl listingId={3} status="ACTIVE" onChange={vi.fn()} />
    )
    await user.click(screen.getByRole('radio', { name: 'Available' }))
    expect(updateListingStatus).not.toHaveBeenCalled()
  })
})
