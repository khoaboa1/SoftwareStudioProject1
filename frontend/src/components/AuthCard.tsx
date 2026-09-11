import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from './Logo'

type AuthCardProps = {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="flex min-h-dvh items-center justify-center bg-zinc-50 px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-4 text-center">
          <Link to="/" aria-label="Handoff home">
            <Logo className="h-10 w-10" />
          </Link>
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">{title}</h1>
            <p className="mt-1 text-sm text-zinc-500">{subtitle}</p>
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
          {children}
        </div>

        <p className="mt-6 text-center text-sm text-zinc-500">{footer}</p>
      </div>
    </div>
  )
}
