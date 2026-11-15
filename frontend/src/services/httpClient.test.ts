import { afterEach, expect, it, vi } from 'vitest'
import { ApiError, NETWORK_ERROR_MESSAGE, NetworkError, request } from './httpClient'

afterEach(() => vi.unstubAllGlobals())

it('omits JSON content type for FormData bodies', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  await request('/upload', { method: 'POST', body: new FormData() })
  expect(fetchMock.mock.calls[0][1].headers['Content-Type']).toBeUndefined()
})

it('keeps JSON content type for JSON bodies', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  await request('/listing', { method: 'POST', body: '{}' })
  expect(fetchMock.mock.calls[0][1].headers['Content-Type']).toBe('application/json')
})

function paths(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.map((call) => new URL(call[0] as string).pathname)
}

it('sends cookies and never an Authorization header', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  await request('/auth/me')
  expect(fetchMock.mock.calls[0][1].credentials).toBe('include')
  expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined()
})

it('turns a failed fetch into a NetworkError', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
  const error = await request('/listings').catch((caught: unknown) => caught)
  expect(error).toBeInstanceOf(NetworkError)
  expect(error).toBeInstanceOf(ApiError)
  expect((error as NetworkError).status).toBe(0)
  expect((error as NetworkError).message).toBe(NETWORK_ERROR_MESSAGE)
})

it('refreshes the session once and retries after a 401', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(null, { status: 401 }))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(new Response('{"ok":true}', { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)
  await expect(request('/listings')).resolves.toEqual({ ok: true })
  expect(paths(fetchMock)).toEqual(['/listings', '/auth/refresh', '/listings'])
  expect(fetchMock.mock.calls[1][1].method).toBe('POST')
})

it('returns the original 401 when the refresh fails', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response('{"message":"Log in first"}', { status: 401 }))
    .mockResolvedValueOnce(new Response(null, { status: 401 }))
  vi.stubGlobal('fetch', fetchMock)
  const error = await request('/listings').catch((caught: unknown) => caught)
  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(401)
  expect((error as ApiError).message).toBe('Log in first')
  expect(paths(fetchMock)).toEqual(['/listings', '/auth/refresh'])
})

it('does not refresh after a rejected auth action', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response('{"message":"Invalid email or password"}', { status: 401 }))
  vi.stubGlobal('fetch', fetchMock)
  await expect(request('/auth/login', { method: 'POST', body: '{}' })).rejects.toMatchObject({ status: 401 })
  expect(fetchMock).toHaveBeenCalledTimes(1)
})
