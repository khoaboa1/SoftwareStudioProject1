import { ApiError } from '../services/httpClient'

/** Returns the API message or the supplied fallback for other failures. */
export function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

/** Checks whether a failure is an API error with the given status. */
export function hasApiStatus(error: unknown, status: number): boolean {
  return error instanceof ApiError && error.status === status
}
