import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import { ReportListingPanel } from './ReportListingPanel'
import { reportListing } from '../services/reportService'
import { ApiError } from '../services/httpClient'

vi.mock('../services/reportService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/reportService')>()),
  reportListing: vi.fn(),
}))

it('requires a reason, sends details, and calls onDone', async () => {
  const user = userEvent.setup()
  const onDone = vi.fn()
  vi.mocked(reportListing).mockResolvedValue({
    id: 2,
    listingId: 7,
    reason: 'SCAM',
    details: 'Fake',
    status: 'OPEN',
    createdAt: '',
  })
  render(
    <ReportListingPanel listingId={7} onDone={onDone} onCancel={vi.fn()} />
  )
  expect(screen.getByRole('button', { name: 'Send report' })).toBeDisabled()
  await user.click(screen.getByRole('radio', { name: 'Scam or fraud' }))
  await user.type(
    screen.getByRole('textbox', { name: 'Details (optional)' }),
    'Fake'
  )
  expect(screen.getByText('4/500')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Send report' }))
  expect(reportListing).toHaveBeenCalledWith(7, {
    reason: 'SCAM',
    details: 'Fake',
  })
  expect(onDone).toHaveBeenCalledOnce()
})

it('sends undefined for blank details and disables repeat submission', async () => {
  vi.mocked(reportListing).mockReturnValue(new Promise(() => {}))
  render(
    <ReportListingPanel listingId={7} onDone={vi.fn()} onCancel={vi.fn()} />
  )
  await userEvent.setup().click(screen.getByRole('radio', { name: 'Spam' }))
  const send = screen.getByRole('button', { name: 'Send report' })
  await userEvent.setup().click(send)
  expect(reportListing).toHaveBeenCalledWith(7, {
    reason: 'SPAM',
    details: undefined,
  })
  expect(send).toBeDisabled()
})

it('shows the backend error in the panel', async () => {
  vi.mocked(reportListing).mockRejectedValue(
    new ApiError(409, 'You have already reported this listing')
  )
  render(
    <ReportListingPanel listingId={7} onDone={vi.fn()} onCancel={vi.fn()} />
  )
  await userEvent.setup().click(screen.getByRole('radio', { name: 'Spam' }))
  await userEvent
    .setup()
    .click(screen.getByRole('button', { name: 'Send report' }))
  expect(
    await screen.findByText('You have already reported this listing')
  ).toBeInTheDocument()
})
