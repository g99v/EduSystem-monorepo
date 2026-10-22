import { createContext, useContext } from 'react'
import type { AuthUser, RegisterStudentRequest } from '../api/auth'

export type AuthContextValue = {
  user: AuthUser | null
  loading: boolean
  bootstrapError: string | null
  signIn: (email: string, password: string) => Promise<AuthUser>
  register: (request: RegisterStudentRequest) => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('AuthProvider is missing')
  return context
}
