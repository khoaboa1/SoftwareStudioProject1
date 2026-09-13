import type { ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './lib/auth-context'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { FeedPage } from './pages/FeedPage'

function RequireAuth({ children }: { children: ReactElement }) {
  const { student, loading } = useAuth()
  if (loading) return null
  if (!student) return <Navigate to="/login" replace />
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
      </Routes>
    </AuthProvider>
  )
}

export default App
