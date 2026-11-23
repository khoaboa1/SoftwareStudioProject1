import { expect, it } from 'vitest'
import { isAdmin } from './roles'
import type { Student } from '../services/authService'

const student = {
  id: 1,
  studentName: 'Jane',
  email: 'jane@tulane.edu',
} as Student

it('recognizes only an explicit ADMIN role', () => {
  expect(isAdmin({ ...student, role: 'ADMIN' })).toBe(true)
  expect(isAdmin({ ...student, role: 'STUDENT' })).toBe(false)
  expect(isAdmin(student)).toBe(false)
  expect(isAdmin(null)).toBe(false)
})
