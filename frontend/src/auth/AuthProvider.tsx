import { useEffect, useState, type ReactNode } from 'react'
import { AuthContext } from './AuthContext'
import {
  errorMessage, getCurrentUser, isStatus, loginUser, logoutUser,
  registerStudent, type AuthUser, type RegisterStudentRequest,
} from '../api/auth'
import { getAuthSession, onAuthChange, restoreAuthSession } from '../api/client'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [bootstrapError, setBootstrapError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    const unsubscribe = onAuthChange(setUser)

    async function restoreSession() {
      try {
        const stored = restoreAuthSession()
        if (stored) {
          const current = await getCurrentUser()
          if (!cancelled && getAuthSession()?.accessToken === stored.accessToken) setUser(current)
        }
      } catch (error) {
        if (!cancelled && !isStatus(error, 401)) {
          setBootstrapError(errorMessage(error))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void restoreSession()
    return () => {
      cancelled = true
      unsubscribe()
    }
  }, [])

  async function signIn(email: string, password: string) {
    const current = await loginUser(email, password)
    setBootstrapError(null)
    return current
  }

  async function register(request: RegisterStudentRequest) {
    await registerStudent(request)
  }

  async function signOut() {
    await logoutUser()
  }

  return (
    <AuthContext.Provider value={{ user, loading, bootstrapError, signIn, register, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}
