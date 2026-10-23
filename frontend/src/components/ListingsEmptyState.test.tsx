import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { ListingsEmptyState } from './ListingsEmptyState'

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname + location.search}</span>
}

function renderEmptyState(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/feed" element={<><ListingsEmptyState /><LocationProbe /></>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ListingsEmptyState', () => {
  it('shows the no-listings message without a clear button when no filters are set', () => {
    renderEmptyState('/feed')

    expect(screen.getByText('No listings yet. Be the first to post one!')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Clear Filters' })).not.toBeInTheDocument()
  })

  it('does not treat the page number as a filter', () => {
    renderEmptyState('/feed?page=2')

    expect(screen.getByText('No listings yet. Be the first to post one!')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Clear Filters' })).not.toBeInTheDocument()
  })

  it('shows Clear Filters when filters are in the URL', () => {
    renderEmptyState('/feed?q=lamp&category=DECOR')

    expect(screen.getByRole('heading', { name: 'No listings match your filters' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Clear Filters' })).toBeInTheDocument()
  })

  it('clears all query params but keeps the path', async () => {
    const user = userEvent.setup()
    renderEmptyState('/feed?q=lamp&category=DECOR')

    await user.click(screen.getByRole('button', { name: 'Clear Filters' }))

    expect(screen.getByTestId('location')).toHaveTextContent(/^\/feed$/)
  })
})
