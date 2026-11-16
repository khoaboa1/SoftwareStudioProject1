import { afterEach, expect, it, vi } from 'vitest'
import {
  changeStudentRole,
  dismissReport,
  getAdminStudents,
  getOpenReports,
  suspendStudent,
  unsuspendStudent,
} from './adminService'

afterEach(() => vi.unstubAllGlobals())

it('calls the report queue and dismiss endpoints', async () => {
  const fetchMock = vi
    .fn()
    .mockResolvedValueOnce(new Response('[]'))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
  vi.stubGlobal('fetch', fetchMock)
  await getOpenReports()
  await dismissReport(3)
  expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/admin\/reports$/)
  expect(fetchMock.mock.calls[1][0]).toMatch(
    /\/api\/admin\/reports\/3\/dismiss$/
  )
  expect(fetchMock.mock.calls[1][1].method).toBe('POST')
})

it('omits blank search and sends pagination', async () => {
  const fetchMock = vi
    .fn()
    .mockResolvedValue(new Response('{"items":[],"meta":{}}'))
  vi.stubGlobal('fetch', fetchMock)
  await getAdminStudents({ q: '  ', page: 2, limit: 20 })
  expect(fetchMock.mock.calls[0][0]).toMatch(
    /\/api\/admin\/students\?page=2&limit=20$/
  )
})

it('calls student action endpoints with the required methods and role body', async () => {
  const fetchMock = vi
    .fn()
    .mockImplementation(() => Promise.resolve(new Response('{}')))
  vi.stubGlobal('fetch', fetchMock)
  await suspendStudent(4)
  await unsuspendStudent(4)
  await changeStudentRole(4, 'ADMIN')
  expect(
    fetchMock.mock.calls.map(([url, options]) => [
      new URL(url).pathname,
      options.method,
    ])
  ).toEqual([
    ['/api/admin/students/4/suspend', 'POST'],
    ['/api/admin/students/4/unsuspend', 'POST'],
    ['/api/admin/students/4/role', 'PUT'],
  ])
  expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toEqual({ role: 'ADMIN' })
})
