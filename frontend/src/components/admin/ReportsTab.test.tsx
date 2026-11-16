import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { ReportsTab } from './ReportsTab'
import { dismissReport, getOpenReports } from '../../services/adminService'
import { deleteListing } from '../../services/listingService'
import { ApiError } from '../../services/httpClient'

vi.mock('../../services/adminService', () => ({
  getOpenReports: vi.fn(),
  dismissReport: vi.fn(),
}))
vi.mock('../../services/listingService', () => ({ deleteListing: vi.fn() }))
const reports = [
  {
    id: 1,
    reason: 'SCAM' as const,
    details: 'Fake',
    createdAt: '2026-11-14T00:00:00Z',
    reporterId: 2,
    reporterName: 'Jane',
    listingId: 5,
    itemName: 'Lamp',
    sellerId: 3,
    sellerName: 'Sam',
    sellerSuspended: false,
  },
  {
    id: 2,
    reason: 'SPAM' as const,
    details: null,
    createdAt: '2026-11-14T00:00:00Z',
    reporterId: 4,
    reporterName: 'Pat',
    listingId: 5,
    itemName: 'Lamp',
    sellerId: 3,
    sellerName: 'Sam',
    sellerSuspended: true,
  },
]
function renderTab() {
  return render(
    <MemoryRouter>
      <ReportsTab />
    </MemoryRouter>
  )
}
beforeEach(() => {
  vi.mocked(getOpenReports).mockReset()
  vi.mocked(dismissReport).mockReset()
  vi.mocked(deleteListing).mockReset()
})

it('lists report details and removes a dismissed row', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  vi.mocked(dismissReport).mockResolvedValue(undefined)
  renderTab()
  const row = await screen.findByTestId('report-1')
  expect(within(row).getByText('Fake')).toBeInTheDocument()
  expect(within(row).getByText('Scam or fraud')).toBeInTheDocument()
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Dismiss' }))
  expect(screen.queryByTestId('report-1')).not.toBeInTheDocument()
})

it('shows the empty queue', async () => {
  vi.mocked(getOpenReports).mockResolvedValue([])
  renderTab()
  expect(await screen.findByText('No open reports.')).toBeInTheDocument()
})

it('deleting a listing removes every report for it', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  vi.mocked(deleteListing).mockResolvedValue(undefined)
  renderTab()
  const row = await screen.findByTestId('report-1')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Delete listing' }))
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Delete' }))
  expect(deleteListing).toHaveBeenCalledWith(5)
  expect(screen.queryByTestId('report-1')).not.toBeInTheDocument()
  expect(screen.queryByTestId('report-2')).not.toBeInTheDocument()
})

it('keeps an action error on its row', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  vi.mocked(dismissReport).mockRejectedValue(
    new ApiError(409, 'Cannot dismiss report')
  )
  renderTab()
  const row = await screen.findByTestId('report-1')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Dismiss' }))
  expect(
    await within(row).findByText('Cannot dismiss report')
  ).toBeInTheDocument()
})

it('marks Dismiss busy while the request is pending', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  vi.mocked(dismissReport).mockReturnValue(new Promise(() => {}))
  renderTab()
  const row = await screen.findByTestId('report-1')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Dismiss' }))
  expect(
    within(row).getByRole('button', { name: 'Please wait…' })
  ).toHaveAttribute('aria-busy', 'true')
})

it('keeps both rows removed when concurrent dismisses finish', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  let finishFirst!: () => void
  let finishSecond!: () => void
  vi.mocked(dismissReport).mockImplementation(
    (id) =>
      new Promise<void>((resolve) => {
        if (id === 1) finishFirst = resolve
        else finishSecond = resolve
      })
  )
  renderTab()
  const first = await screen.findByTestId('report-1')
  const second = screen.getByTestId('report-2')
  await userEvent
    .setup()
    .click(within(first).getByRole('button', { name: 'Dismiss' }))
  await userEvent
    .setup()
    .click(within(second).getByRole('button', { name: 'Dismiss' }))
  await act(async () => finishFirst())
  await act(async () => finishSecond())
  expect(screen.queryByTestId('report-1')).not.toBeInTheDocument()
  expect(screen.queryByTestId('report-2')).not.toBeInTheDocument()
})

it('reports the open count on load and after a dismiss', async () => {
  vi.mocked(getOpenReports).mockResolvedValue(reports)
  vi.mocked(dismissReport).mockResolvedValue(undefined)
  const onCountChange = vi.fn()
  render(
    <MemoryRouter>
      <ReportsTab onCountChange={onCountChange} />
    </MemoryRouter>
  )
  const row = await screen.findByTestId('report-1')
  expect(onCountChange).toHaveBeenLastCalledWith(2)
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Dismiss' }))
  expect(onCountChange).toHaveBeenLastCalledWith(1)
})
