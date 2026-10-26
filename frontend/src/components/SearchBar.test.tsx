import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SearchBar } from './SearchBar'

describe('SearchBar', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function setup(value = '') {
    const onSearch = vi.fn()
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    render(<SearchBar value={value} onSearch={onSearch} />)
    return { onSearch, user }
  }

  it('debounces typing and searches 400ms after the last keystroke', async () => {
    const { onSearch } = setup()
    const input = screen.getByRole('searchbox', { name: 'Search listings' })
    fireEvent.change(input, { target: { value: 'lam' } })
    act(() => vi.advanceTimersByTime(399))
    fireEvent.change(input, { target: { value: 'lamp' } })
    expect(onSearch).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(399))
    expect(onSearch).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('lamp')
  })

  it('searches immediately on Enter and does not fire again after the delay', async () => {
    const { onSearch, user } = setup()
    await user.type(screen.getByRole('searchbox', { name: 'Search listings' }), 'desk{Enter}')
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('desk')
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).toHaveBeenCalledTimes(1)
  })

  it('trims whitespace before searching', async () => {
    const { onSearch, user } = setup()
    await user.type(screen.getByRole('searchbox', { name: 'Search listings' }), '  lamp  ')
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('lamp')
  })

  it('does not search for whitespace-only input when the value is already empty', async () => {
    const { onSearch, user } = setup()
    await user.type(screen.getByRole('searchbox', { name: 'Search listings' }), '   ')
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('shows the clear button only when there is text', async () => {
    const { user } = setup()
    expect(screen.queryByRole('button', { name: 'Clear search' })).not.toBeInTheDocument()
    await user.type(screen.getByRole('searchbox', { name: 'Search listings' }), 'a')
    expect(screen.getByRole('button', { name: 'Clear search' })).toBeInTheDocument()
  })

  it("clear button empties the input, searches for '' and refocuses", async () => {
    const { onSearch, user } = setup('lamp')
    const input = screen.getByRole('searchbox', { name: 'Search listings' })
    await user.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(input).toHaveValue('')
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('')
    expect(input).toHaveFocus()
    expect(screen.queryByRole('button', { name: 'Clear search' })).not.toBeInTheDocument()
  })

  it('syncs the input when value changes from outside', () => {
    const onSearch = vi.fn()
    const { rerender } = render(<SearchBar value="lamp" onSearch={onSearch} />)
    rerender(<SearchBar value="" onSearch={onSearch} />)
    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('')
  })

  it('does not restore a pending search after the URL value is cleared', async () => {
    const onSearch = vi.fn()
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const { rerender } = render(<SearchBar value="lamp" onSearch={onSearch} />)
    await user.type(screen.getByRole('searchbox', { name: 'Search listings' }), 'b')
    rerender(<SearchBar value="" onSearch={onSearch} />)
    act(() => vi.advanceTimersByTime(400))

    expect(screen.getByRole('searchbox', { name: 'Search listings' })).toHaveValue('')
    expect(onSearch).not.toHaveBeenCalled()
  })
})
