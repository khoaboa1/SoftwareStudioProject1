import { ApiError, request } from './httpClient'

export { ApiError }

export type Profile = {
  id: number
  name: string
  major: string
  bio: string | null
  schoolDomain: string
  studentId: number
}

export function getMyProfile(): Promise<Profile> {
  return request<Profile>('/api/profiles/me')
}

export async function getMyProfileOrNull(): Promise<Profile | null> {
  try {
    return await getMyProfile()
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null
    }
    throw error
  }
}

export function createProfile(input: {
  name: string
  major: string
  bio?: string
}): Promise<Profile> {
  return request<Profile>('/api/profiles', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
