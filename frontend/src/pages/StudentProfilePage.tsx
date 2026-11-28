import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { MyListings } from '../components/MyListings'
import { ProfileView } from '../components/ProfileView'
import { useAuth } from '../lib/auth-context'
import { getErrorMessage, hasApiStatus } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getPublicProfile } from '../services/profileService'

const NOT_FOUND = 'This profile could not be found.'

/** Read-only profile of another student at the same school, with their listings. */
export function StudentProfilePage() {
  const { studentId } = useParams()
  const id = Number(studentId)
  const validId = Number.isInteger(id) && id > 0
  const navigate = useNavigate()
  const { student, setStudent } = useAuth()
  const isOwn = student?.id === id

  const { state } = useAsyncResource(() => getPublicProfile(id), [id], {
    enabled: validId && !isOwn,
    onError: (error) => {
      if (hasApiStatus(error, 401)) {
        setStudent(null)
        navigate('/login', { replace: true })
        return true
      }
      return false
    },
  })

  if (isOwn) return <Navigate to="/profile" replace />

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-3xl px-4 py-8">
        {!validId ? (
          <ProfileView status="error" error={NOT_FOUND} />
        ) : state.status === 'ready' ? (
          <>
            <ProfileView
              status="ready"
              profile={state.data}
              verified={state.data.verified}
            />
            <MyListings
              sellerId={state.data.studentId}
              ownerName={state.data.name}
            />
          </>
        ) : state.status === 'error' ? (
          <ProfileView
            status="error"
            error={
              hasApiStatus(state.error, 404)
                ? NOT_FOUND
                : getErrorMessage(state.error, 'Could not load this profile.')
            }
          />
        ) : (
          <ProfileView status="loading" />
        )}
      </main>
    </div>
  )
}
