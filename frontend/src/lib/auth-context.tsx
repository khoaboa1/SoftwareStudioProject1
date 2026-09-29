import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { getCurrentStudent, type Student } from '../services/authService'
import { getMyProfileOrNull } from '../services/profileService'

type AuthContextValue = {
  student: Student | null
  hasProfile: boolean | null
  loading: boolean
  setStudent: (student: Student | null) => void
  markProfileComplete: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [student, setStudentState] = useState<Student | null>(null)
  const [hasProfile, setHasProfile] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    getCurrentStudent().then(async (current) => {
      if (cancelled) return
      setStudentState(current)
      if (current) {
        const profile = await getMyProfileOrNull()
        if (!cancelled) setHasProfile(profile !== null)
      }
      if (!cancelled) setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [])

  function setStudent(next: Student | null) {
    setStudentState(next)
    if (next) {
      setHasProfile(null)
      getMyProfileOrNull().then((profile) => setHasProfile(profile !== null))
    } else {
      setHasProfile(null)
    }
  }

  function markProfileComplete() {
    setHasProfile(true)
  }

  return (
    <AuthContext.Provider value={{ student, hasProfile, loading, setStudent, markProfileComplete }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
