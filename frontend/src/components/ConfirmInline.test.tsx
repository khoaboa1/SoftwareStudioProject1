import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import { ConfirmInline } from './ConfirmInline'
import { ApiError } from '../services/httpClient'

it('confirms once while pending and shows an inline backend error', async () => {
  const onConfirm = vi
    .fn()
    .mockRejectedValue(new ApiError(409, 'Cannot delete listing'))
  render(
    <ConfirmInline
      message="Delete this listing for everyone?"
      confirmLabel="Delete"
      onConfirm={onConfirm}
      onCancel={vi.fn()}
    />
  )
  await userEvent.setup().click(screen.getByRole('button', { name: 'Delete' }))
  expect(onConfirm).toHaveBeenCalledOnce()
  expect(await screen.findByText('Cannot delete listing')).toBeInTheDocument()
})

it('cancels without confirming', async () => {
  const onConfirm = vi.fn()
  const onCancel = vi.fn()
  render(
    <ConfirmInline
      message="Delete this listing for everyone?"
      confirmLabel="Delete"
      onConfirm={onConfirm}
      onCancel={onCancel}
    />
  )
  await userEvent.setup().click(screen.getByRole('button', { name: 'Cancel' }))
  expect(onCancel).toHaveBeenCalledOnce()
  expect(onConfirm).not.toHaveBeenCalled()
})
