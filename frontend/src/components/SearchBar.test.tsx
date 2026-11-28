import { act, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SearchBar } from './SearchBar'
import {
  searchProfiles,
  type ProfileSearchItem,
} from '../services/profileService'
vi.mock('../services/profileService', () => ({ searchProfiles: vi.fn() }))
const jane: ProfileSearchItem = {
  id: 3,
  studentId: 8,
  name: 'Jane Doe',
  major: 'CS',
  bio: null,
  email: 'jane@tulane.edu',
  schoolDomain: 'tulane.edu',
}
const janet: ProfileSearchItem = {
  id: 4,
  studentId: 9,
  name: 'Janet Smith',
  major: 'Math',
  bio: null,
  email: 'janet@tulane.edu',
  schoolDomain: 'tulane.edu',
}
function setupMentions() {
  const onSearch = vi.fn()
  const onSelectProfile = vi.fn()
  const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
  render(
    <SearchBar value="" onSearch={onSearch} onSelectProfile={onSelectProfile} />
  )
  const input = screen.getByRole('searchbox', { name: 'Search listings' })
  return { onSearch, onSelectProfile, user, input }
}

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
    await user.type(
      screen.getByRole('searchbox', { name: 'Search listings' }),
      'desk{Enter}'
    )
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('desk')
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).toHaveBeenCalledTimes(1)
  })

  it('trims whitespace before searching', async () => {
    const { onSearch, user } = setup()
    await user.type(
      screen.getByRole('searchbox', { name: 'Search listings' }),
      '  lamp  '
    )
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('lamp')
  })

  it('does not search for whitespace-only input when the value is already empty', async () => {
    const { onSearch, user } = setup()
    await user.type(
      screen.getByRole('searchbox', { name: 'Search listings' }),
      '   '
    )
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('shows the clear button only when there is text', async () => {
    const { user } = setup()
    expect(
      screen.queryByRole('button', { name: 'Clear search' })
    ).not.toBeInTheDocument()
    await user.type(
      screen.getByRole('searchbox', { name: 'Search listings' }),
      'a'
    )
    expect(
      screen.getByRole('button', { name: 'Clear search' })
    ).toBeInTheDocument()
  })

  it("clear button empties the input, searches for '' and refocuses", async () => {
    const { onSearch, user } = setup('lamp')
    const input = screen.getByRole('searchbox', { name: 'Search listings' })
    await user.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(input).toHaveValue('')
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('')
    expect(input).toHaveFocus()
    expect(
      screen.queryByRole('button', { name: 'Clear search' })
    ).not.toBeInTheDocument()
  })

  it('syncs the input when value changes from outside', () => {
    const onSearch = vi.fn()
    const { rerender } = render(<SearchBar value="lamp" onSearch={onSearch} />)
    rerender(<SearchBar value="" onSearch={onSearch} />)
    expect(
      screen.getByRole('searchbox', { name: 'Search listings' })
    ).toHaveValue('')
  })

  it('does not restore a pending search after the URL value is cleared', async () => {
    const onSearch = vi.fn()
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const { rerender } = render(<SearchBar value="lamp" onSearch={onSearch} />)
    await user.type(
      screen.getByRole('searchbox', { name: 'Search listings' }),
      'b'
    )
    rerender(<SearchBar value="" onSearch={onSearch} />)
    act(() => vi.advanceTimersByTime(400))

    expect(
      screen.getByRole('searchbox', { name: 'Search listings' })
    ).toHaveValue('')
    expect(onSearch).not.toHaveBeenCalled()
  })
})

describe('SearchBar @ student suggestions', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.mocked(searchProfiles).mockReset()
  })
  afterEach(() => {
    vi.useRealTimers()
  })
  it('mentions @ in the placeholder', () => {
    const { input } = setupMentions()
    expect(input).toHaveAttribute(
      'placeholder',
      'Search listings, or type @ to find students…'
    )
  })
  it('looks up students after the debounce instead of searching listings', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane, janet])
    const { input, onSearch } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(399))
    expect(searchProfiles).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(searchProfiles).toHaveBeenCalledExactlyOnceWith('jan')
    const list = await screen.findByRole('listbox', {
      name: 'Matching students',
    })
    const options = within(list).getAllByRole('option')
    expect(options).toHaveLength(2)
    expect(within(options[0]).getByText('Jane Doe')).toBeInTheDocument()
    expect(within(options[0]).getByText('jane@tulane.edu')).toBeInTheDocument()
    expect(within(options[0]).getByText('tulane.edu')).toBeInTheDocument()
    expect(onSearch).not.toHaveBeenCalled()
  })
  it('selects a student on click', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane, janet])
    const { input, user, onSearch, onSelectProfile } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    await user.click(await screen.findByRole('option', { name: /Jane Doe/ }))
    expect(onSelectProfile).toHaveBeenCalledExactlyOnceWith(jane)
    expect(input).toHaveValue('@Jane Doe')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(onSearch).not.toHaveBeenCalled()
  })
  it('selects with the arrow keys and Enter', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane, janet])
    const { input, onSearch, onSelectProfile } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    await screen.findByRole('listbox')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    const option = screen.getByRole('option', { name: /Janet Smith/ })
    expect(option).toHaveAttribute('aria-selected', 'true')
    expect(input).toHaveAttribute('aria-activedescendant', option.id)
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(onSelectProfile).toHaveBeenCalledExactlyOnceWith(janet)
    expect(onSearch).not.toHaveBeenCalled()
  })
  it('forgets the highlighted student when the query changes', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane, janet])
    const { input, onSelectProfile } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    await screen.findByRole('listbox')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.change(input, { target: { value: '@janet' } })
    expect(input).not.toHaveAttribute('aria-activedescendant')
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(onSelectProfile).not.toHaveBeenCalled()
  })
  it('closes the list on Escape', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane])
    const { input, onSelectProfile } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    await screen.findByRole('listbox')
    fireEvent.keyDown(input, { key: 'Escape' })
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(onSelectProfile).not.toHaveBeenCalled()
  })
  it('does not look anyone up for a bare @', () => {
    const { input, onSearch } = setupMentions()
    fireEvent.change(input, { target: { value: '@' } })
    act(() => vi.advanceTimersByTime(400))
    expect(searchProfiles).not.toHaveBeenCalled()
    expect(onSearch).not.toHaveBeenCalled()
  })
  it('does not search listings when Enter is pressed on an @ query', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane])
    const { input, user, onSearch, onSelectProfile } = setupMentions()
    await user.type(input, '@jan{Enter}')
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).not.toHaveBeenCalled()
    expect(onSelectProfile).not.toHaveBeenCalled()
  })
  it('says when no students match', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([])
    const { input } = setupMentions()
    fireEvent.change(input, { target: { value: '@zzz' } })
    act(() => vi.advanceTimersByTime(400))
    expect(await screen.findByText('No students found')).toBeInTheDocument()
  })
  it('says when the lookup fails', async () => {
    vi.mocked(searchProfiles).mockRejectedValue(new Error('down'))
    const { input } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    expect(
      await screen.findByText('Could not search students')
    ).toBeInTheDocument()
  })
  it('ignores a late answer for an older query', async () => {
    let resolveFirst: (items: ProfileSearchItem[]) => void = () => {}
    vi.mocked(searchProfiles)
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveFirst = resolve
        })
      )
      .mockResolvedValueOnce([janet])
    const { input } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    fireEvent.change(input, { target: { value: '@janet' } })
    act(() => vi.advanceTimersByTime(400))
    await screen.findByRole('option', { name: /Janet Smith/ })
    await act(async () => resolveFirst([jane]))
    expect(
      screen.queryByRole('option', { name: /Jane Doe/ })
    ).not.toBeInTheDocument()
  })
  it('clearing the box closes the list', async () => {
    vi.mocked(searchProfiles).mockResolvedValue([jane])
    const { input, user } = setupMentions()
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    await screen.findByRole('listbox')
    await user.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })
  it('keeps @ as plain listing search when onSelectProfile is not given', () => {
    const onSearch = vi.fn()
    render(<SearchBar value="" onSearch={onSearch} />)
    const input = screen.getByRole('searchbox', { name: 'Search listings' })
    fireEvent.change(input, { target: { value: '@jan' } })
    act(() => vi.advanceTimersByTime(400))
    expect(onSearch).toHaveBeenCalledExactlyOnceWith('@jan')
    expect(searchProfiles).not.toHaveBeenCalled()
    expect(input).toHaveAttribute('placeholder', 'Search listings…')
  })
})
