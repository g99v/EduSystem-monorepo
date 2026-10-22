import type { AuthUser } from '../api/auth'

export function homeForRole(role: AuthUser['role']) {
  if (role === 'STUDENT') return '/student'
  if (role === 'INSTRUCTOR') return '/instructor'
  return '/unsupported-role'
}
