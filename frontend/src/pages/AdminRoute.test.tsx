import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { expect, it, vi } from 'vitest'
import { RequireAdmin } from '../App'
import { AdminPage } from './AdminPage'
import { getOpenReports } from '../services/adminService'

const authState = vi.hoisted(() => ({ role: 'STUDENT' as 'STUDENT' | 'ADMIN' }))
vi.mock('../lib/auth-context', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
  useAuth: () => ({
    student: { id: 1, role: authState.role },
    hasProfile: true,
    loading: false,
  }),
}))
vi.mock('../components/AppHeader', () => ({ AppHeader: () => <header /> }))
vi.mock('../services/adminService', () => ({
  getOpenReports: vi.fn().mockResolvedValue([]),
  getAdminStudents: vi.fn().mockResolvedValue({
    items: [],
    meta: { totalItems: 0, currentPage: 1, totalPages: 0, pageSize: 20 },
  }),
}))

function renderRoute() {
  render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminPage />
            </RequireAdmin>
          }
        />
        <Route path="/feed" element={<h1>Feed page</h1>} />
      </Routes>
    </MemoryRouter>
  )
}

it('RequireAdmin redirects a student to /feed', () => {
  authState.role = 'STUDENT'
  vi.mocked(getOpenReports).mockClear()
  renderRoute()
  expect(screen.getByRole('heading', { name: 'Feed page' })).toBeInTheDocument()
  expect(getOpenReports).not.toHaveBeenCalled()
})

it('RequireAdmin renders the page for an admin', () => {
  authState.role = 'ADMIN'
  renderRoute()
  expect(screen.getByRole('heading', { name: 'Admin' })).toBeInTheDocument()
})
