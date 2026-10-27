import { describe, expect, it } from 'vitest'
import { ApiError } from '../services/httpClient'
import { getErrorMessage, hasApiStatus } from './apiError'

describe('getErrorMessage', () => {
  it('returns the API message for an ApiError', () => {
    expect(getErrorMessage(new ApiError(400, 'Price must be positive'), 'fallback')).toBe('Price must be positive')
  })

  it('returns the fallback for a network error', () => {
    expect(getErrorMessage(new TypeError('Failed to fetch'), 'Could not load your profile.')).toBe('Could not load your profile.')
  })

  it('returns the fallback for non-Error values', () => {
    expect(getErrorMessage(undefined, 'fallback')).toBe('fallback')
    expect(getErrorMessage('boom', 'fallback')).toBe('fallback')
  })
})

describe('hasApiStatus', () => {
  it('matches only an ApiError with that status', () => {
    expect(hasApiStatus(new ApiError(404, 'x'), 404)).toBe(true)
    expect(hasApiStatus(new ApiError(500, 'x'), 404)).toBe(false)
    expect(hasApiStatus(new Error('x'), 404)).toBe(false)
  })
})
