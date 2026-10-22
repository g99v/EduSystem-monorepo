import type { ReactNode } from 'react'

type AuthLayoutProps = {
  title: string
  children: ReactNode
}

export function AuthLayout({ title, children }: AuthLayoutProps) {
  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1>{title}</h1>
        {children}
      </div>
    </div>
  )
}
