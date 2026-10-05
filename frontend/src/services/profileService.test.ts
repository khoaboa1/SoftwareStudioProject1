import { afterEach, describe, expect, it, vi } from 'vitest'
import { updateProfile } from './profileService'

describe('updateProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('PUTs the changes to the profile by id and returns the updated profile', async () => {
    const updated = { id: 7, name: 'Jane Smith', major: 'Data Science', bio: '' }
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(updated), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await updateProfile(7, { name: 'Jane Smith', major: 'Data Science', bio: '' })

    expect(result).toEqual(updated)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/profiles\/7$/)
    expect(options.method).toBe('PUT')
    expect(options.credentials).toBe('include')
    expect(JSON.parse(options.body)).toEqual({ name: 'Jane Smith', major: 'Data Science', bio: '' })
  })
})
