import { describe, expect, it } from 'vitest'
import { studentFromLogin } from './authAdapter'
import type { LoginResult } from './authService'

const success: LoginResult = {
  requiresPin: false,
  message: 'Login successful',
  id: 1,
  studentName: 'Jane Doe',
  email: 'jane@tulane.edu',
  emailVerified: true,
  sellingItems: null,
  dormLocation: null,
}

describe('studentFromLogin', () => {
  it('carries the verified status through from a successful login', () => {
    expect(studentFromLogin({ ...success, verified: true })?.verified).toBe(true)
  })

  it('keeps an unverified status as false', () => {
    expect(studentFromLogin({ ...success, verified: false })?.verified).toBe(false)
  })

  it('returns null when the login still requires a PIN', () => {
    expect(studentFromLogin({ requiresPin: true, message: 'PIN sent', email: 'jane@tulane.edu' })).toBeNull()
  })
})
