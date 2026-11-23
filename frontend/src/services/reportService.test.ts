import { afterEach, expect, it, vi } from 'vitest'
import { reportListing } from './reportService'

afterEach(() => vi.unstubAllGlobals())

it('posts a report with its reason and optional details', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  await reportListing(7, { reason: 'SCAM', details: 'Fake item' })
  const [url, options] = fetchMock.mock.calls[0]
  expect(url).toMatch(/\/listings\/7\/reports$/)
  expect(options.method).toBe('POST')
  expect(JSON.parse(options.body)).toEqual({
    reason: 'SCAM',
    details: 'Fake item',
  })
})
