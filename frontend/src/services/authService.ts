import { ApiError, request } from './httpClient'

export { ApiError }

export type Student = {
  id: number
  studentName: string
  email: string
  emailVerified: boolean
  sellingItems: string[] | null
  dormLocation: string | null
}

export type LoginResult = {
  requiresPin: boolean
  message: string
  id?: number
  studentName?: string
  email?: string
  emailVerified?: boolean
  sellingItems?: string[] | null
  dormLocation?: string | null
}

export function signup(input: {
  studentName: string
  email: string
  password: string
}): Promise<Student> {
  return request<Student>('/auth/signup', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function login(input: {
  email: string
  password: string
  deviceId?: string
}): Promise<LoginResult> {
  return request<LoginResult>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function verifyPin(input: {
  email: string
  pin: string
  deviceId?: string
}): Promise<Student> {
  return request<Student>('/auth/verify-pin', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function resendPin(input: { email: string }): Promise<void> {
  return request<void>('/auth/resend-pin', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function logout(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' })
}

export async function getCurrentStudent(): Promise<Student | null> {
  try {
    return await request<Student>('/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export const fetchCurrentUser = getCurrentStudent
