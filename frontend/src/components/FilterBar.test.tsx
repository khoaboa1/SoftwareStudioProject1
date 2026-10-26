import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { FilterBar } from './FilterBar'
import type { BrowseFilters } from '../lib/browseParams'

const defaults: BrowseFilters = { q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest' }

describe('FilterBar', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  function setup(filters: BrowseFilters = defaults) {
    const onChange = vi.fn()
    const onClear = vi.fn()
    render(<FilterBar filters={filters} onChange={onChange} onClear={onClear} />)
    return { onChange, onClear }
  }

  it('shows the current filters', () => {
    setup({ ...defaults, category: 'DECOR', minPrice: 5, maxPrice: 20, sort: 'price_desc' })
    expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('decor')
    expect(screen.getByRole('spinbutton', { name: 'Min price' })).toHaveValue(5)
    expect(screen.getByRole('spinbutton', { name: 'Max price' })).toHaveValue(20)
    expect(screen.getByRole('combobox', { name: 'Sort by' })).toHaveValue('price_desc')
  })

  it('changing category reports it immediately', () => {
    const { onChange } = setup()
    fireEvent.change(screen.getByRole('combobox', { name: 'Category' }), { target: { value: 'kitchen' } })
    expect(onChange).toHaveBeenLastCalledWith({ category: 'kitchen' })
    fireEvent.change(screen.getByRole('combobox', { name: 'Category' }), { target: { value: '' } })
    expect(onChange).toHaveBeenLastCalledWith({ category: null })
  })

  it('changing sort reports it immediately', () => {
    const { onChange } = setup()
    fireEvent.change(screen.getByRole('combobox', { name: 'Sort by' }), { target: { value: 'price_asc' } })
    expect(onChange).toHaveBeenLastCalledWith({ sort: 'price_asc' })
    fireEvent.change(screen.getByRole('combobox', { name: 'Sort by' }), { target: { value: 'newest' } })
    expect(onChange).toHaveBeenLastCalledWith({ sort: null })
  })

  it('debounces price input', () => {
    const { onChange } = setup()
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Min price' }), { target: { value: '15' } })
    act(() => vi.advanceTimersByTime(399))
    expect(onChange).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(onChange).toHaveBeenCalledExactlyOnceWith({ minPrice: '15', maxPrice: null })
  })

  it('blocks min greater than max', () => {
    const { onChange } = setup()
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Min price' }), { target: { value: '50' } })
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Max price' }), { target: { value: '10' } })
    act(() => vi.advanceTimersByTime(400))
    expect(onChange).not.toHaveBeenCalled()
    for (const name of ['Min price', 'Max price']) {
      const input = screen.getByRole('spinbutton', { name })
      expect(input).toHaveAttribute('aria-invalid', 'true')
      expect(input).toHaveAccessibleDescription("Min price can't be more than max price")
    }
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Max price' }), { target: { value: '60' } })
    act(() => vi.advanceTimersByTime(400))
    expect(onChange).toHaveBeenCalledExactlyOnceWith({ minPrice: '50', maxPrice: '60' })
    expect(screen.queryByText("Min price can't be more than max price")).not.toBeInTheDocument()
    for (const name of ['Min price', 'Max price']) {
      expect(screen.getByRole('spinbutton', { name })).toHaveAttribute('aria-invalid', 'false')
    }
  })

  it('shows the price error with red outlines and an overlaid message that takes no layout space', () => {
    setup()
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Min price' }), { target: { value: '50' } })
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Max price' }), { target: { value: '10' } })
    act(() => vi.advanceTimersByTime(400))
    const message = screen.getByText("Min price can't be more than max price")
    expect(message).not.toHaveClass('sr-only')
    expect(message).toHaveClass('absolute')
    for (const name of ['Min price', 'Max price']) {
      expect(screen.getByRole('spinbutton', { name }).className).toMatch(/border-red-400!/)
    }
  })

  it('Clear filters shows only when active and calls onClear', () => {
    const { onClear } = setup()
    expect(screen.queryByRole('button', { name: 'Clear filters' })).not.toBeInTheDocument()
    render(<FilterBar filters={{ ...defaults, category: 'DECOR' }} onChange={vi.fn()} onClear={onClear} />)
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
    expect(onClear).toHaveBeenCalledTimes(1)
  })
})
