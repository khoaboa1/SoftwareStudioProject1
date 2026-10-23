import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination'

function show(currentPage: number, totalPages: number, totalItems: number, onPageChange = vi.fn()) {
  render(<Pagination currentPage={currentPage} totalPages={totalPages} totalItems={totalItems} onPageChange={onPageChange} />)
  return onPageChange
}

describe('Pagination', () => {
  it('shows the current page and total results', () => {
    show(2, 5, 112)
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent('Page 2 of 5 · 112 results')
  })

  it('uses singular result', () => {
    show(1, 1, 1)
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent('Page 1 of 1 · 1 result')
  })

  it('disables Previous on page 1', () => {
    show(1, 5, 112)
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('disables Next on the last page', () => {
    show(5, 5, 112)
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
  })

  it('disables both on a single page', () => {
    show(1, 1, 7)
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('calls onPageChange with the neighbor page', async () => {
    const onPageChange = show(2, 5, 112)
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Next' }))
    await user.click(screen.getByRole('button', { name: 'Previous' }))
    expect(onPageChange).toHaveBeenNthCalledWith(1, 3)
    expect(onPageChange).toHaveBeenNthCalledWith(2, 1)
  })

  it('renders nothing with zero results', () => {
    show(1, 0, 0)
    expect(screen.queryByRole('navigation', { name: 'Pagination' })).toBeNull()
  })
})
