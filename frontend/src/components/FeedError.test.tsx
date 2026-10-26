import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { FeedError } from './FeedError'

describe('FeedError', () => {
  it('shows a friendly message', () => {
    render(<FeedError onRetry={vi.fn()} />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent("We couldn't load listings")
    expect(alert).toHaveTextContent('Check your connection and try again.')
  })

  it('calls onRetry when Retry is clicked', async () => {
    const onRetry = vi.fn()
    render(<FeedError onRetry={onRetry} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})
