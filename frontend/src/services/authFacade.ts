import { getOrCreateDeviceId } from '../lib/device'
import { studentFromLogin } from './authAdapter'
import { login, logout, verifyPin, type Student } from './authService'

/** Coordinates transport, device identity, and auth-state updates for sign-in clients. */
export function createAuthFacade(setStudent: (student: Student | null) => void) {
  return {
    async signIn(input: { email: string; password: string }) {
      const result = await login({ ...input, deviceId: getOrCreateDeviceId() })
      const student = studentFromLogin(result)
      if (student) setStudent(student)
      return result
    },

    async verifySignIn(input: { email: string; pin: string }) {
      const student = await verifyPin({ ...input, deviceId: getOrCreateDeviceId() })
      setStudent(student)
      return student
    },

    async signOut() {
      await logout()
      setStudent(null)
    },
  }
}
