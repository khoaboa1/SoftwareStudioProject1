import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { getCurrentStudent, type Student } from '../services/authService'

type AuthContextValue = {
  student: Student | null
  loading: boolean
  setStudent: (student: Student | null) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [student, setStudent] = useState<Student | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    getCurrentStudent().then((current) => {
      if (!cancelled) {
        setStudent(current)
        setLoading(false)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <AuthContext.Provider value={{ student, loading, setStudent }}>
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
