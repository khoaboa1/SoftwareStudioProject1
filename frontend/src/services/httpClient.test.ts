import { afterEach, expect, it, vi } from 'vitest'
import { request } from './httpClient'

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
