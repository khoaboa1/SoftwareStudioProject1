import type { LoginResult, Student } from './authService'

/** Adapts the login endpoint's flat response to the student shape used by auth state. */
export function studentFromLogin(result: LoginResult): Student | null {
  if (result.requiresPin || !result.id || !result.studentName || !result.email) return null

  return {
    id: result.id,
    studentName: result.studentName,
    email: result.email,
    emailVerified: result.emailVerified ?? true,
    sellingItems: result.sellingItems ?? null,
    dormLocation: result.dormLocation ?? null,
  }
}
