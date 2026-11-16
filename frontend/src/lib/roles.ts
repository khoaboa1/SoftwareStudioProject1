import type { Student } from '../services/authService'

export function isAdmin(student: Student | null | undefined): boolean {
  return student?.role === 'ADMIN'
}
