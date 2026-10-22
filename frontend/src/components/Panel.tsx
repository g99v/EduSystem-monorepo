import type { ReactNode } from 'react'

type PanelProps = {
  title: string
  children: ReactNode
  className?: string
}

export function Panel({ title, children, className = '' }: PanelProps) {
  return (
    <div className={`panel ${className}`}>
      <div className="panel-header">
        <h2>{title}</h2>
      </div>
      {children}
    </div>
  )
}
