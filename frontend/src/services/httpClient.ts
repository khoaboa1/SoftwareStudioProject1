const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
/** Auth actions whose 401 is the real answer, never a reason to refresh the session. */
const NO_REFRESH_PATHS = new Set([
  '/auth/signup',
  '/auth/login',
  '/auth/verify-pin',
  '/auth/resend-pin',
  '/auth/refresh',
  '/auth/logout',
])
export const NETWORK_ERROR_MESSAGE = "Can't reach the server. Check your connection and try again."
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}
/** The request never reached the backend: offline, server down, or blocked. */
export class NetworkError extends ApiError {
  constructor() {
    super(0, NETWORK_ERROR_MESSAGE)
  }
}
let refreshInFlight: Promise<boolean> | null = null
async function send(path: string, options: RequestInit): Promise<Response> {
  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      // The backend keeps the Supabase tokens in HttpOnly cookies; the browser attaches them.
      credentials: 'include',
      headers: {
        ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...options.headers,
      },
    })
  } catch {
    throw new NetworkError()
  }
}
/** Asks the backend to renew the auth cookies. Parallel 401s share one refresh call. */
function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = send('/auth/refresh', { method: 'POST' })
      .then((response) => response.ok)
      .catch(() => false)
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response = await send(path, options)
  if (response.status === 401 && !NO_REFRESH_PATHS.has(path) && (await refreshSession())) {
    response = await send(path, options)
  }
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
