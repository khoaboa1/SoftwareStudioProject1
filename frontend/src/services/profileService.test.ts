import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  getPublicProfile,
  searchProfiles,
  updateProfile,
} from './profileService'

describe('updateProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('PUTs the changes to the profile by id and returns the updated profile', async () => {
    const updated = {
      id: 7,
      name: 'Jane Smith',
      major: 'Data Science',
      bio: '',
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify(updated), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await updateProfile(7, {
      name: 'Jane Smith',
      major: 'Data Science',
      bio: '',
    })

    expect(result).toEqual(updated)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/profiles\/7$/)
    expect(options.method).toBe('PUT')
    expect(options.credentials).toBe('include')
    expect(JSON.parse(options.body)).toEqual({
      name: 'Jane Smith',
      major: 'Data Science',
      bio: '',
    })
  })
})

describe('searchProfiles', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GETs the same-school search with a limit of 5 and returns the items', async () => {
    const item = {
      id: 3,
      studentId: 8,
      name: 'Jane Doe',
      major: 'CS',
      bio: null,
      email: 'jane@tulane.edu',
      schoolDomain: 'tulane.edu',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          items: [item],
          meta: { totalItems: 1, currentPage: 1, totalPages: 1, pageSize: 5 },
        }),
        { status: 200 }
      )
    )
    vi.stubGlobal('fetch', fetchMock)

    const result = await searchProfiles('jan')

    expect(result).toEqual([item])
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/profiles\/search\?q=jan&limit=5$/)
    expect(options.credentials).toBe('include')
  })

  it('URL-encodes the query', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          items: [],
          meta: { totalItems: 0, currentPage: 1, totalPages: 0, pageSize: 5 },
        }),
        { status: 200 }
      )
    )
    vi.stubGlobal('fetch', fetchMock)

    await searchProfiles('jane doe@')

    const [url] = fetchMock.mock.calls[0]
    expect(url).toMatch(/q=jane%20doe%40&limit=5$/)
  })
})

describe('getPublicProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GETs the public profile by student id', async () => {
    const profile = {
      id: 3,
      studentId: 7,
      name: 'Jane Doe',
      major: 'CS',
      bio: null,
      schoolDomain: 'tulane.edu',
      createdAt: '2026-09-22T00:00:00',
      verified: true,
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify(profile), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await getPublicProfile(7)

    expect(result).toEqual(profile)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/profiles\/students\/7$/)
    expect(options.credentials).toBe('include')
  })
})
