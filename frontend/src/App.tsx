import type { ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './lib/auth-context'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { FeedPage } from './pages/FeedPage'
import { ProfileSetupPage } from './pages/ProfileSetupPage'
import { ProfilePage } from './pages/ProfilePage'

function RequireAuth({ children }: { children: ReactElement }) {
  const { student, hasProfile, loading } = useAuth()
  if (loading) return null
  if (!student) return <Navigate to="/login" replace />
  if (hasProfile === null) return null
  if (hasProfile === false) return <Navigate to="/profile-setup" replace />
  return children
}

function RequireAuthNoProfile({ children }: { children: ReactElement }) {
  const { student, hasProfile, loading } = useAuth()
  if (loading) return null
  if (!student) return <Navigate to="/login" replace />
  if (hasProfile === true) return <Navigate to="/feed" replace />
  return children
}

function RedirectIfAuthed({ children }: { children: ReactElement }) {
  const { student, loading } = useAuth()
  if (loading) return null
  if (student) return <Navigate to="/feed" replace />
  return children
}

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/feed" replace />} />
        <Route
          path="/login"
          element={
            <RedirectIfAuthed>
              <LoginPage />
            </RedirectIfAuthed>
          }
        />
        <Route
          path="/signup"
          element={
            <RedirectIfAuthed>
              <SignupPage />
            </RedirectIfAuthed>
          }
        />
        <Route
          path="/feed"
          element={
            <RequireAuth>
              <FeedPage />
            </RequireAuth>
          }
        />
        <Route
          path="/profile"
          element={
            <RequireAuth>
              <ProfilePage />
            </RequireAuth>
          }
        />
        <Route
          path="/profile-setup"
          element={
            <RequireAuthNoProfile>
              <ProfileSetupPage />
            </RequireAuthNoProfile>
          }
        />
      </Routes>
    </AuthProvider>
  )
}

export default App
