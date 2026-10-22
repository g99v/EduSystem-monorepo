import axios from 'axios'
import type { AuthResponse, AuthUser } from './auth'

// Business requests use a bearer token. Login and registration use cookies and CSRF.
export const api = axios.create()

const authApi = axios.create({ withCredentials: true })
const sessionKey = 'edusystem-auth-session'

type CsrfToken = {
  token: string
  headerName: string
}

let csrfToken: CsrfToken | null = null
let session: AuthResponse | null = null
let expiresAt = 0
let expiryTimer: ReturnType<typeof setTimeout> | undefined
let authQueue: Promise<unknown> = Promise.resolve()
let authListener: ((user: AuthUser | null) => void) | null = null

export function onAuthChange(listener: (user: AuthUser | null) => void) {
  authListener = listener
  const checkExpiry = () => { getAuthSession() }
  window.addEventListener('focus', checkExpiry)
  document.addEventListener('visibilitychange', checkExpiry)
  return () => {
    authListener = null
    window.removeEventListener('focus', checkExpiry)
    document.removeEventListener('visibilitychange', checkExpiry)
  }
}

export function setAuthSession(next: AuthResponse | null) {
  clearTimeout(expiryTimer)
  // Use the JWT's original expiry so reloading never extends the login lifetime.
  expiresAt = next ? tokenExpiry(next.accessToken) : 0
  session = expiresAt > Date.now() ? next : null
  try {
    if (session) sessionStorage.setItem(sessionKey, JSON.stringify(session))
    else sessionStorage.removeItem(sessionKey)
  } catch {
    // Login still works in memory when browser storage is unavailable.
  }
  if (session) expiryTimer = setTimeout(() => setAuthSession(null), expiresAt - Date.now())
  authListener?.(session?.user ?? null)
}

function tokenExpiry(token: string): number {
  const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
    throw new Error('Access token is missing its expiry')
  }
  return payload.exp * 1000
}

export function getAuthSession() {
  if (session && expiresAt <= Date.now()) setAuthSession(null)
  return session
}

export function restoreAuthSession() {
  if (session) return getAuthSession()
  try {
    const stored = sessionStorage.getItem(sessionKey)
    if (stored) setAuthSession(JSON.parse(stored) as AuthResponse)
  } catch {
    setAuthSession(null)
  }
  return getAuthSession()
}

async function getCsrfToken() {
  if (csrfToken) return csrfToken
  const response = await authApi.get<CsrfToken>('/api/auth/csrf')
  csrfToken = response.data
  return csrfToken
}

export async function postWithCsrf<T>(url: string, body?: object) {
  async function send() {
    let csrf = await getCsrfToken()
    try {
      return await authApi.post<T>(url, body, {
        headers: { [csrf.headerName]: csrf.token },
      })
    } catch (error) {
      if (!axios.isAxiosError(error) || error.response?.status !== 403) throw error
      // Another tab may have changed the CSRF cookie. Fetch its current value once.
      csrfToken = null
      csrf = await getCsrfToken()
      return authApi.post<T>(url, body, {
        headers: { [csrf.headerName]: csrf.token },
      })
    }
  }

  // Serialize auth writes so the CSRF cookie cannot change during a request.
  if (navigator.locks) return navigator.locks.request('edusystem-auth', send)
  const request = authQueue.catch(() => {}).then(send)
  authQueue = request
  return request
}

api.interceptors.request.use((request) => {
  const current = getAuthSession()
  if (current) request.headers.set('Authorization', `Bearer ${current.accessToken}`)
  else request.headers.delete('Authorization')
  return request
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const current = getAuthSession()
      // A late response from an older login must not clear a newer login.
      if (current && error.config?.headers.get('Authorization') === `Bearer ${current.accessToken}`) {
        setAuthSession(null)
      }
    }
    return Promise.reject(error)
  },
)
