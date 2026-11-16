import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { expect, it, vi } from 'vitest'
import { AdminPage } from './AdminPage'
import { getOpenReports } from '../services/adminService'

vi.mock('../components/AppHeader', () => ({ AppHeader: () => <header /> }))
vi.mock('../services/adminService', () => ({
  getOpenReports: vi.fn().mockResolvedValue([]),
  getAdminStudents: vi.fn().mockResolvedValue({
    items: [],
    meta: { totalItems: 0, currentPage: 1, totalPages: 0, pageSize: 20 },
  }),
}))
vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({ student: { id: 1, role: 'ADMIN' } }),
}))

function Location() {
  const location = useLocation()
  return <p data-testid="location">{location.search}</p>
}

it('switches tabs and updates the URL', async () => {
  render(
    <MemoryRouter initialEntries={['/admin']}>
      <AdminPage />
      <Location />
    </MemoryRouter>
  )
  expect(screen.getByRole('tab', { name: 'Reports' })).toHaveAttribute(
    'aria-selected',
    'true'
  )
  await userEvent.setup().click(screen.getByRole('tab', { name: 'Students' }))
  expect(screen.getByTestId('location')).toHaveTextContent('?tab=students')
  expect(screen.getByRole('tab', { name: 'Students' })).toHaveAttribute(
    'aria-selected',
    'true'
  )
})

it('opens the Students tab from a deep link', async () => {
  render(
    <MemoryRouter initialEntries={['/admin?tab=students']}>
      <AdminPage />
    </MemoryRouter>
  )
  expect(screen.getByRole('tab', { name: 'Students' })).toHaveAttribute(
    'aria-selected',
    'true'
  )
})

it('shows the open report count on the Reports tab, also from the Students tab', async () => {
  vi.mocked(getOpenReports).mockResolvedValueOnce(
    [1, 2].map(
      (id) =>
        ({
          id,
          reason: 'SPAM',
          details: null,
          createdAt: '2026-11-14T00:00:00Z',
          reporterId: 9,
          reporterName: 'Jane',
          listingId: id,
          itemName: 'Lamp',
          sellerId: 3,
          sellerName: 'Sam',
          sellerSuspended: false,
        }) as never
    )
  )
  render(
    <MemoryRouter initialEntries={['/admin?tab=students']}>
      <AdminPage />
    </MemoryRouter>
  )
  expect(
    await screen.findByRole('tab', { name: 'Reports, 2 open reports' })
  ).toBeInTheDocument()
  expect(getOpenReports).toHaveBeenCalledTimes(1)
})
