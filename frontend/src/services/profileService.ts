import { ApiError, request } from './httpClient'
import { hasApiStatus } from '../lib/apiError'

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
    if (hasApiStatus(error, 404)) {
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

export function updateProfile(
  id: number,
  input: ProfileUpdate
): Promise<Profile> {
  return request<Profile>(`/api/profiles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

/** One student in the same-school @ search. */
export type ProfileSearchItem = {
  id: number
  studentId: number
  name: string
  major: string
  bio: string | null
  email: string
  schoolDomain: string
}

type ProfileSearchPage = {
  items: ProfileSearchItem[]
  meta: {
    totalItems: number
    currentPage: number
    totalPages: number
    pageSize: number
  }
}

export async function searchProfiles(
  query: string,
  limit = 5
): Promise<ProfileSearchItem[]> {
  const page = await request<ProfileSearchPage>(
    `/api/profiles/search?q=${encodeURIComponent(query)}&limit=${limit}`
  )
  return page.items
}

/** Another student's read-only profile. */
export type PublicProfile = Profile & { verified: boolean }

export function getPublicProfile(studentId: number): Promise<PublicProfile> {
  return request<PublicProfile>(`/api/profiles/students/${studentId}`)
}
