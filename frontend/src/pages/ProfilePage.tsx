import { AppHeader } from '../components/AppHeader'
import { ProfileView } from '../components/ProfileView'
import type { Profile } from '../services/profileService'

// TODO SSP1-64: replace with getMyProfile() and real loading/error states.
const SAMPLE_PROFILE: Profile = {
  id: 1,
  name: 'Jane Doe',
  major: 'Computer Science',
  bio: 'Junior studying CS and Math. Selling textbooks and dorm gear before I move off campus.',
  schoolDomain: 'tulane.edu',
  studentId: 1,
  createdAt: '2026-09-22T00:00:00',
}

export function ProfilePage() {
  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-3xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold text-zinc-900">My profile</h1>
        <ProfileView status="ready" profile={SAMPLE_PROFILE} />
      </main>
    </div>
  )
}
