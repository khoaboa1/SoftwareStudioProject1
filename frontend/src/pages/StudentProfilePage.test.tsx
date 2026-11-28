import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { StudentProfilePage } from './StudentProfilePage'
import { getListings, type Listing } from '../services/listingService'
import {
  ApiError,
  getPublicProfile,
  type PublicProfile,
} from '../services/profileService'
// Mock the auth context
const setStudent = vi.fn()
vi.mock('../lib/auth-context', () => ({
  useAuth: () => ({
    student: { id: 1, studentName: 'Viewer', email: 'viewer@tulane.edu' },
    setStudent,
  }),
}))
// Mock services
vi.mock('../services/listingService', () => ({ getListings: vi.fn() }))
vi.mock('../services/profileService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/profileService')>()),
  getPublicProfile: vi.fn(),
}))
const jane: PublicProfile = {
  id: 3,
  studentId: 7,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: 'Junior studying CS.',
  schoolDomain: 'tulane.edu',
  createdAt: '2026-09-22T00:00:00',
  verified: true,
}
const listing = (overrides: Partial<Listing>): Listing => ({
  id: 1,
  itemName: 'Test Item',
  description: 'Works great',
  price: 10,
  condition: 'GOOD',
  category: 'FURNITURE',
  sellerId: 7,
  sellerName: 'Jane Doe',
  createdAt: '2026-09-22T00:00:00Z',
  ...overrides,
})
const renderPage = (path = '/students/7') => {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/students/:studentId" element={<StudentProfilePage />} />
        <Route path="/profile" element={<p>My profile screen</p>} />
        <Route path="/login" element={<p>Login screen</p>} />
      </Routes>
    </MemoryRouter>
  )
}
describe('StudentProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getPublicProfile).mockReset()
    vi.mocked(getListings).mockReset()
    setStudent.mockReset()
    vi.mocked(getListings).mockResolvedValue([])
  })
  test("shows the student's profile without an edit button", async () => {
    vi.mocked(getPublicProfile).mockResolvedValue(jane)
    renderPage()
    expect(
      await screen.findByRole('heading', { name: 'Jane Doe' })
    ).toBeInTheDocument()
    expect(screen.getByText('Computer Science')).toBeInTheDocument()
    expect(screen.getByText('tulane.edu')).toBeInTheDocument()
    expect(screen.getByText('Junior studying CS.')).toBeInTheDocument()
    expect(screen.getByText('Verified Student')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Edit profile' })
    ).not.toBeInTheDocument()
    expect(getPublicProfile).toHaveBeenCalledWith(7)
  })
  test('lists their listings with links to each item', async () => {
    vi.mocked(getPublicProfile).mockResolvedValue(jane)
    vi.mocked(getListings).mockResolvedValue([
      listing({ id: 1, itemName: 'Desk Lamp', sellerId: 7 }),
      listing({ id: 2, itemName: 'Old Bike', sellerId: 7, status: 'SOLD' }),
      listing({ id: 5, itemName: 'Mini Fridge', sellerId: 99 }),
    ])
    renderPage()
    expect(
      await screen.findByRole('heading', { name: "Jane Doe's listings (1)" })
    ).toBeInTheDocument()
    const deskLampLink = screen.getByRole('link', { name: 'Desk Lamp' })
    expect(deskLampLink).toHaveAttribute('href', '/listings/1')
    expect(screen.queryByText('Old Bike')).not.toBeInTheDocument()
    expect(screen.queryByText('Mini Fridge')).not.toBeInTheDocument()
  })
  test('sends you to your own profile for your own id', async () => {
    renderPage('/students/1')
    expect(await screen.findByText('My profile screen')).toBeInTheDocument()
    expect(getPublicProfile).not.toHaveBeenCalled()
  })
  test('sends you to login on 401', async () => {
    vi.mocked(getPublicProfile).mockRejectedValue(
      new ApiError(401, 'Unauthorized')
    )
    renderPage()
    expect(await screen.findByText('Login screen')).toBeInTheDocument()
    expect(setStudent).toHaveBeenCalledWith(null)
  })
  test('says the profile could not be found on 404', async () => {
    vi.mocked(getPublicProfile).mockRejectedValue(
      new ApiError(404, 'Profile not found')
    )
    renderPage()
    expect(
      await screen.findByText('This profile could not be found.')
    ).toBeInTheDocument()
  })
  test('shows the error message for other failures', async () => {
    vi.mocked(getPublicProfile).mockRejectedValue(
      new ApiError(500, 'Server down')
    )
    renderPage()
    expect(await screen.findByText('Server down')).toBeInTheDocument()
  })
  test('treats a non-numeric id as not found', async () => {
    renderPage('/students/abc')
    expect(
      await screen.findByText('This profile could not be found.')
    ).toBeInTheDocument()
    expect(getPublicProfile).not.toHaveBeenCalled()
  })
})
