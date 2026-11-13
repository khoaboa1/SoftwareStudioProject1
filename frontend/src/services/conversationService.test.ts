import { afterEach, describe, expect, it, vi } from 'vitest'
import { getConversation, getMessages, sendMessage } from './conversationService'

function stubFetch(body: unknown, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('conversationService chat calls', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('getConversation GETs one conversation by id', async () => {
    const conversation = { id: 5, listingId: 2, itemName: 'Desk Lamp' }
    const fetchMock = stubFetch(conversation)

    expect(await getConversation(5)).toEqual(conversation)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/conversations\/5$/)
    expect(options.method ?? 'GET').toBe('GET')
    expect(options.credentials).toBe('include')
  })

  it('getMessages GETs the full history when no cursor is given', async () => {
    const fetchMock = stubFetch([])

    expect(await getMessages(5)).toEqual([])
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/conversations\/5\/messages$/)
  })

  it('getMessages asks only for messages after the given id when polling', async () => {
    const fetchMock = stubFetch([])

    await getMessages(5, 12)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/conversations\/5\/messages\?after=12$/)
  })

  it('sendMessage POSTs the content and returns the saved message', async () => {
    const saved = { id: 13, conversationId: 5, senderId: 1, senderName: 'Jane Doe', content: 'Hi', createdAt: '2026-11-13T10:00:00Z' }
    const fetchMock = stubFetch(saved, 201)

    expect(await sendMessage(5, 'Hi')).toEqual(saved)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/conversations\/5\/messages$/)
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual({ content: 'Hi' })
  })
})
