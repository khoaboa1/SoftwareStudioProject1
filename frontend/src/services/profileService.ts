import { ApiError, request } from './httpClient'

export { ApiError }

export type Profile = {
  id: number
  name: string
  major: string
  bio: string | null
  schoolDomain: string
  studentId: number
  createdAt: string
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

export type ProfileUpdate = {
  name: string
  major: string
  bio: string
}

export function updateProfile(id: number, input: ProfileUpdate): Promise<Profile> {
  return request<Profile>(`/api/profiles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}
