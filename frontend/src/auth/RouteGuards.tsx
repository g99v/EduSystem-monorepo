import type { ReactNode } from 'react'
import { Link, Navigate, } from 'react-router-dom'
import type { AuthUser } from '../api/auth'
import { useAuth } from './AuthContext'
import { homeForRole } from './routeAccess'

export function GuestRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="page-loading">Loading...</div>
  if (user) return <Navigate to={homeForRole(user.role)} replace />
  return children
}

export function RequireRole({ role, children }: { role: AuthUser['role']; children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) {
    return <div className="page-loading">Loading...</div>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (user.role !== role) {
    return (
      <div className="access-message">
        <h1>Access denied</h1>
        <Link to={homeForRole(user.role)}>Go to your dashboard</Link>
      </div>
    )
  }


  return children
}
