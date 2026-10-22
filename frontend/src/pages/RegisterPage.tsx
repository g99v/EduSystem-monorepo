import { useState, type SubmitEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { useAuth } from '../auth/AuthContext'
import { errorMessage, fieldErrors, isStatus } from '../api/auth'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const schoolId = String(form.get('schoolId') ?? '').trim()
    setBusy(true)
    setError('')
    setErrors({})
    try {
      await register({
        firstName: String(form.get('firstName')).trim(),
        lastName: String(form.get('lastName')).trim(),
        email: String(form.get('email')).trim(),
        password: String(form.get('password')),
        age: Number(form.get('age')),
        schoolId: schoolId ? Number(schoolId) : null,
      })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (cause) {
      const validation = fieldErrors(cause)
      if (isStatus(cause, 409)) validation.email = errorMessage(cause)
      if (Object.keys(validation).length > 0) setErrors(validation)
      else setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Register as a Student">
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="field">
            <label htmlFor="firstName">First name</label>
            <input id="firstName" name="firstName" autoComplete="given-name" maxLength={20} required />
            {errors.firstName && <small className="field-error">{errors.firstName}</small>}
          </div>
          <div className="field">
            <label htmlFor="lastName">Last name</label>
            <input id="lastName" name="lastName" autoComplete="family-name" maxLength={255} required />
            {errors.lastName && <small className="field-error">{errors.lastName}</small>}
          </div>
        </div>
        <label htmlFor="registerEmail">Email</label>
        <input id="registerEmail" name="email" type="email" autoComplete="email" maxLength={255} required />
        {errors.email && <small className="field-error">{errors.email}</small>}
        <div className="form-row">
          <div className="field">
            <label htmlFor="age">Age</label>
            <input id="age" name="age" type="number" min="0" step="1" required />
            {errors.age && <small className="field-error">{errors.age}</small>}
          </div>
          <div className="field">
            <label htmlFor="schoolId">School ID (optional)</label>
            <input id="schoolId" name="schoolId" type="number" min="1" step="1" />
            {errors.schoolId && <small className="field-error">{errors.schoolId}</small>}
          </div>
        </div>
        <label htmlFor="registerPassword">Password</label>
        <input id="registerPassword" name="password" type="password" autoComplete="new-password" minLength={8} required />
        {errors.password && <small className="field-error">{errors.password}</small>}
        {error && <p className="form-notice">{error}</p>}
        <button className="primary-button" type="submit" disabled={busy}>
          {busy ? 'Registering...' : 'Register'}
        </button>
      </form>
      <Link className="auth-link" to="/login">Sign in</Link>
    </AuthLayout>
  )
}
