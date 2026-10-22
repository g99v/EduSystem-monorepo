import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/auth'
import { useAuth } from '../auth/AuthContext'

export function UnsupportedRolePage() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  if (!user) return null

  async function handleLogout() {
    try {
      await signOut()
      navigate('/login', { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    }
  }

  return (
    <div className="access-message">
      <h1>No dashboard for this role</h1>
      <button type="button" onClick={handleLogout}>Log out</button>
      {error && <p>{error}</p>}
    </div>
  )
}
