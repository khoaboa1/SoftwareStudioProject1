import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { StudentsTab } from './StudentsTab'
import {
  changeStudentRole,
  getAdminStudents,
  suspendStudent,
} from '../../services/adminService'
import { ApiError } from '../../services/httpClient'

vi.mock('../../lib/auth-context', () => ({
  useAuth: () => ({ student: { id: 1, role: 'ADMIN' } }),
}))
vi.mock('../../services/adminService', () => ({
  getAdminStudents: vi.fn(),
  changeStudentRole: vi.fn(),
  suspendStudent: vi.fn(),
  unsuspendStudent: vi.fn(),
}))
const own = {
  id: 1,
  name: 'Me',
  email: 'me@tulane.edu',
  role: 'ADMIN' as const,
  suspended: false,
  createdAt: '',
}
const admin = {
  id: 2,
  name: 'Alex',
  email: 'alex@tulane.edu',
  role: 'ADMIN' as const,
  suspended: false,
  createdAt: '',
}
const student = {
  id: 3,
  name: 'Sam',
  email: 'sam@tulane.edu',
  role: 'STUDENT' as const,
  suspended: false,
  createdAt: '',
}
function renderTab() {
  return render(
    <MemoryRouter>
      <StudentsTab />
    </MemoryRouter>
  )
}
beforeEach(() => {
  vi.mocked(getAdminStudents).mockReset()
  vi.mocked(changeStudentRole).mockReset()
  vi.mocked(suspendStudent).mockReset()
  vi.mocked(getAdminStudents).mockResolvedValue({
    items: [own, admin, student],
    meta: { totalItems: 3, currentPage: 1, totalPages: 2, pageSize: 2 },
  })
})

it('hides actions on own row and disables Suspend for another admin with a reason', async () => {
  renderTab()
  const me = await screen.findByTestId('student-1')
  expect(within(me).getByText('You')).toBeInTheDocument()
  expect(within(me).queryByRole('button')).not.toBeInTheDocument()
  const other = screen.getByTestId('student-2')
  expect(within(other).getByRole('button', { name: 'Suspend' })).toBeDisabled()
  expect(within(other).getByText('Remove admin role first')).toBeInTheDocument()
})

it('replaces the suspended student row in place', async () => {
  vi.mocked(suspendStudent).mockResolvedValue({ ...student, suspended: true })
  renderTab()
  const row = await screen.findByTestId('student-3')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Suspend' }))
  expect(await within(row).findByText('Suspended')).toBeInTheDocument()
  expect(
    within(row).getByRole('button', { name: 'Unsuspend' })
  ).toBeInTheDocument()
})

it('replaces the role badge after Make admin', async () => {
  vi.mocked(changeStudentRole).mockResolvedValue({ ...student, role: 'ADMIN' })
  renderTab()
  const row = await screen.findByTestId('student-3')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Make admin' }))
  expect(await within(row).findByText('Admin')).toBeInTheDocument()
})

it('sends search and page parameters', async () => {
  renderTab()
  await screen.findByTestId('student-3')
  await userEvent.setup().type(screen.getByRole('searchbox'), 'Alex{enter}')
  expect(getAdminStudents).toHaveBeenCalledWith(
    expect.objectContaining({ q: 'Alex', page: 1 })
  )
  await userEvent.setup().click(screen.getByRole('button', { name: 'Next' }))
  expect(getAdminStudents).toHaveBeenCalledWith(
    expect.objectContaining({ page: 2 })
  )
})

it('shows a backend action error on its row', async () => {
  vi.mocked(suspendStudent).mockRejectedValue(
    new ApiError(409, 'Remove admin role before suspending this student')
  )
  renderTab()
  const row = await screen.findByTestId('student-3')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Suspend' }))
  expect(
    await within(row).findByText(
      'Remove admin role before suspending this student'
    )
  ).toBeInTheDocument()
})

it('marks a pending student action busy and prevents another request', async () => {
  vi.mocked(suspendStudent).mockReturnValue(new Promise(() => {}))
  renderTab()
  const row = await screen.findByTestId('student-3')
  await userEvent
    .setup()
    .click(within(row).getByRole('button', { name: 'Suspend' }))
  const pending = within(row).getByRole('button', { name: 'Please wait…' })
  expect(pending).toHaveAttribute('aria-busy', 'true')
  await userEvent.setup().dblClick(pending)
  expect(suspendStudent).toHaveBeenCalledTimes(1)
})

it('preserves updates from concurrent actions on different students', async () => {
  let finishSuspend!: (value: typeof student) => void
  let finishRole!: (
    value:
      | typeof admin
      | {
          id: number
          name: string
          email: string
          role: 'STUDENT'
          suspended: boolean
          createdAt: string
        }
  ) => void
  vi.mocked(suspendStudent).mockImplementation(
    () =>
      new Promise((resolve) => {
        finishSuspend = resolve
      })
  )
  vi.mocked(changeStudentRole).mockImplementation(
    () =>
      new Promise((resolve) => {
        finishRole = resolve
      })
  )
  renderTab()
  const studentRow = await screen.findByTestId('student-3')
  const adminRow = screen.getByTestId('student-2')
  await userEvent
    .setup()
    .click(within(studentRow).getByRole('button', { name: 'Suspend' }))
  await userEvent
    .setup()
    .click(within(adminRow).getByRole('button', { name: 'Remove admin' }))
  await act(async () => finishSuspend({ ...student, suspended: true }))
  await act(async () => finishRole({ ...admin, role: 'STUDENT' }))
  expect(within(studentRow).getByText('Suspended')).toBeInTheDocument()
  expect(within(adminRow).queryByText('Admin')).not.toBeInTheDocument()
})
