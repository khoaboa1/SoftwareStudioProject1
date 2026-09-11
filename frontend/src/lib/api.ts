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
  sellingItems: string[] | null
  dormLocation: string | null
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

export function login(input: { email: string; password: string }): Promise<Student> {
  return request<Student>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function logout(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' })
}
