const API_BASE_URL = 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`
    try {
      const body = await response.json()
      if (body?.message) message = body.message
    } catch {
      // no JSON body to parse — keep the default message
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

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

export type Condition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'WORN'
export type Category = 'FURNITURE' | 'ELECTRONICS' | 'KITCHEN' | 'DECOR' | 'CLOTHING' | 'OTHER'

export type Listing = {
  id: number
  itemName: string
  description: string
  price: number
  condition: Condition
  category: Category
  sellerId: number
  sellerName: string
  createdAt: string
}

export function getListings(): Promise<Listing[]> {
  return request<Listing[]>('/listings')
}

export function createListing(input: {
  itemName: string
  description: string
  price: number
  condition: Condition
  category: Category
}): Promise<Listing> {
  return request<Listing>('/listings', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
