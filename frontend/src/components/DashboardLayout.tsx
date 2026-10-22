import { useEffect, useState, type ReactNode } from 'react'
import { Moon, Sun, type LucideIcon } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/auth'

export type DashboardSection = {
  id: string
  label: string
  icon: LucideIcon
}

type DashboardLayoutProps = {
  sections: DashboardSection[]
  activeSection: string
  onSectionChange: (section: string) => void
  children: ReactNode
}

export function DashboardLayout({ sections, activeSection, onSectionChange, children }: DashboardLayoutProps) {
  const navigate = useNavigate()
  const { signOut } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [darkMode, setDarkMode] = useState(
    document.documentElement.dataset.theme === 'dark',
  )

  useEffect(() => {
    const theme = darkMode ? 'dark' : 'light'
    document.documentElement.dataset.theme = theme
    localStorage.setItem('theme', theme)
  }, [darkMode])

  async function handleLogout() {
    setLoggingOut(true)
    setLogoutError('')
    try {
      await signOut()
      navigate('/login', { replace: true })
    } catch (error) {
      setLogoutError(errorMessage(error))
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <div className="dashboard-shell">
      <div className="sidebar">
        <Link className="brand" to="/login">
          EduSystem
        </Link>
        <div className="sidebar-nav">
          {sections.map((section) => {
            const Icon = section.icon

            return (
              <button
                key={section.id}
                className={`sidebar-link ${activeSection === section.id ? 'active' : ''}`}
                type="button"
                onClick={() => onSectionChange(section.id)}
              >
                <Icon className="nav-icon" />
                {section.label}
              </button>
            )
          })}
        </div>
      </div>
      <div className="dashboard-body">
        <div className="topbar">
          {logoutError && (
            <span className="topbar-error">
              {logoutError}
            </span>
          )}
          <div className="topbar-actions">
            <button
              className="theme-button"
              type="button"
              title={darkMode ? 'Use light mode' : 'Use dark mode'}
              onClick={() => setDarkMode((current) => !current)}
            >
              {darkMode ? (
                <Sun />
              ) : (
                <Moon />
              )}
            </button>
            <button
              className="logout-button"
              type="button"
              onClick={handleLogout}
              disabled={loggingOut}
            >
              Log out
            </button>
          </div>
        </div>
        <div className="dashboard-content">
          {children}
        </div>
      </div>
    </div>
  )
}
