import { request } from './httpClient'
import type { ReportReason } from './reportService'

export type AdminReport = {
  id: number
  reason: ReportReason
  details: string | null
  createdAt: string
  reporterId: number
  reporterName: string
  listingId: number
  itemName: string
  sellerId: number
  sellerName: string
  sellerSuspended: boolean
}

export type AdminStudent = {
  id: number
  name: string
  email: string
  role: 'STUDENT' | 'ADMIN'
  suspended: boolean
  createdAt: string
}

export type AdminStudentPage = {
  items: AdminStudent[]
  meta: {
    totalItems: number
    currentPage: number
    totalPages: number
    pageSize: number
  }
}

export function getOpenReports(): Promise<AdminReport[]> {
  return request('/api/admin/reports')
}

export function dismissReport(id: number): Promise<void> {
  return request(`/api/admin/reports/${id}/dismiss`, { method: 'POST' })
}

export function getAdminStudents(params: {
  q?: string
  page?: number
  limit?: number
}): Promise<AdminStudentPage> {
  const query = new URLSearchParams()
  if (params.q?.trim()) query.set('q', params.q.trim())
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.limit !== undefined) query.set('limit', String(params.limit))
  const suffix = query.size ? `?${query}` : ''
  return request(`/api/admin/students${suffix}`)
}

export function suspendStudent(id: number): Promise<AdminStudent> {
  return request(`/api/admin/students/${id}/suspend`, { method: 'POST' })
}

export function unsuspendStudent(id: number): Promise<AdminStudent> {
  return request(`/api/admin/students/${id}/unsuspend`, { method: 'POST' })
}

export function changeStudentRole(
  id: number,
  role: 'STUDENT' | 'ADMIN'
): Promise<AdminStudent> {
  return request(`/api/admin/students/${id}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role }),
  })
}
