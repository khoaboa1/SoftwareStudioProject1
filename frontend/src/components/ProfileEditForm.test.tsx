import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProfileEditForm } from './ProfileEditForm'
import type { Profile } from '../services/profileService'

const profile: Profile = {
  id: 1,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: 'Junior studying CS and Math.',
  schoolDomain: 'tulane.edu',
  studentId: 1,
  createdAt: '2026-09-22T00:00:00',
}

function renderForm(overrides: Partial<Parameters<typeof ProfileEditForm>[0]> = {}) {
  const props = {
    profile,
    status: 'idle' as const,
    error: null,
    onSubmit: vi.fn(),
    onCancel: vi.fn(),
    ...overrides,
  }
  render(<ProfileEditForm {...props} />)
  return props
}

describe('ProfileEditForm', () => {
  it('starts with the current profile values', () => {
    renderForm()

    expect(screen.getByLabelText('Name')).toHaveValue('Jane Doe')
    expect(screen.getByLabelText('Major')).toHaveValue('Computer Science')
    expect(screen.getByLabelText('Bio')).toHaveValue('Junior studying CS and Math.')
  })

  it('starts with an empty bio when the profile has none', () => {
    renderForm({ profile: { ...profile, bio: null } })

    expect(screen.getByLabelText('Bio')).toHaveValue('')
  })

  it('submits the trimmed values', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.clear(screen.getByLabelText('Major'))
    await user.type(screen.getByLabelText('Major'), '  Data Science  ')
    await user.clear(screen.getByLabelText('Bio'))
    await user.type(screen.getByLabelText('Bio'), '  Senior now.  ')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Jane Doe', major: 'Data Science', bio: 'Senior now.' })
  })

  it('submits an empty bio so it can be cleared', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.clear(screen.getByLabelText('Bio'))
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Jane Doe', major: 'Computer Science', bio: '' })
  })

  it('requires a name', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.clear(screen.getByLabelText('Name'))
    await user.type(screen.getByLabelText('Name'), '   ')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(screen.getByText('Name is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('requires a major', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.clear(screen.getByLabelText('Major'))
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(screen.getByText('Major is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('shows how many bio characters are used', () => {
    renderForm()

    expect(screen.getByText('28/1000')).toBeInTheDocument()
  })

  it('rejects a bio over 1000 characters', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm({ profile: { ...profile, bio: 'a'.repeat(1001) } })

    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(screen.getByText('Bio cannot exceed 1000 characters')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('calls onCancel when cancel is clicked', async () => {
    const user = userEvent.setup()
    const { onCancel, onSubmit } = renderForm()

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('shows the server error', () => {
    renderForm({ status: 'error', error: 'You do not have permission to modify this profile.' })

    expect(screen.getByRole('status')).toHaveTextContent('You do not have permission to modify this profile.')
  })

  it('disables saving while submitting', () => {
    renderForm({ status: 'submitting' })

    expect(screen.getByRole('button', { name: 'Please wait…' })).toBeDisabled()
  })
})
