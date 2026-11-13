import { afterEach, describe, expect, it, vi } from 'vitest'
import { getConversation, getMessages, sendMessage, sortNewestFirst, toSummary, type Conversation } from './conversationService'

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

function conv(id: number, createdAt: string, overrides: Partial<Conversation> = {}): Conversation {
  return { id, listingId: id, itemName: `Item ${id}`, buyerId: 1, buyerName: 'Jane Doe', sellerId: 7, sellerName: 'Sam Seller', createdAt, ...overrides }
}

describe('toSummary last message', () => {
  it('labels my own last message "You"', () => {
    const summary = toSummary(conv(1, '2026-11-01T00:00:00Z', { lastMessage: { content: 'Hi', senderId: 1, createdAt: '2026-11-02T00:00:00Z' } }), 1)
    expect(summary.lastMessage).toEqual({ sender: 'You', text: 'Hi' })
    expect(summary.activityAt).toBe('2026-11-02T00:00:00Z')
  })

  it('labels the other person by first name', () => {
    const summary = toSummary(conv(1, '2026-11-01T00:00:00Z', { lastMessage: { content: 'Yes', senderId: 7, createdAt: '2026-11-02T00:00:00Z' } }), 1)
    expect(summary.lastMessage).toEqual({ sender: 'Sam', text: 'Yes' })
  })

  it('uses the buyer as the other person when I am the seller', () => {
    const summary = toSummary(conv(1, '2026-11-01T00:00:00Z', { buyerId: 9, buyerName: 'Bob Buyer', sellerId: 1, sellerName: 'Jane Doe', lastMessage: { content: 'Hello', senderId: 9, createdAt: '2026-11-02T00:00:00Z' } }), 1)
    expect(summary.lastMessage).toEqual({ sender: 'Bob', text: 'Hello' })
  })

  it('has no last message and falls back to the creation time when none was sent', () => {
    const summary = toSummary(conv(1, '2026-11-01T00:00:00Z'), 1)
    expect(summary.lastMessage).toBeNull()
    expect(summary.activityAt).toBe('2026-11-01T00:00:00Z')
  })
})

describe('sortNewestFirst', () => {
  it('orders by last activity, not by creation time', () => {
    const older = conv(1, '2026-11-01T00:00:00Z', { lastMessage: { content: 'new news', senderId: 7, createdAt: '2026-11-10T00:00:00Z' } })
    const newer = conv(2, '2026-11-05T00:00:00Z')
    expect(sortNewestFirst([newer, older]).map((c) => c.id)).toEqual([1, 2])
  })
})
