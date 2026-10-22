import { useState, type SubmitEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { useAuth } from '../auth/AuthContext'
import { homeForRole } from '../auth/routeAccess'
import { errorMessage } from '../api/auth'

export function LoginPage() {
  const { signIn, bootstrapError } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const registered = Boolean((location.state as { registered?: boolean } | null)?.registered)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true)
    setError('')
    try {
      const user = await signIn(String(form.get('email')).trim(), String(form.get('password')))
      navigate(homeForRole(user.role), { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Sign in">
      <form className="auth-form" onSubmit={handleSubmit}>
        <label htmlFor="email">Email</label>
        <input id="email" name="email" type="email" autoComplete="email" required />
        <label htmlFor="password">Password</label>
        <input id="password" name="password" type="password" autoComplete="current-password" required />
        {registered && !error && (
          <p className="form-success">Registration complete. Sign in.</p>
        )}
        {(error || bootstrapError) && (
          <p className="form-notice">{error || bootstrapError}</p>
        )}
        <button className="primary-button" type="submit" disabled={busy}>
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <Link className="auth-link" to="/register">Register as a Student</Link>
    </AuthLayout>
  )
}
