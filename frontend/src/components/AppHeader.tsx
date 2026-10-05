import { NavLink, useNavigate } from 'react-router-dom'
import { Logo } from './Logo'
import { Button } from './ui/Button'
import { useAuth } from '../lib/auth-context'
import { logout } from '../services/authService'

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `whitespace-nowrap text-sm font-medium ${isActive ? 'text-accent-700' : 'text-zinc-600 hover:text-zinc-900'}`
}

export function AppHeader() {
  const navigate = useNavigate()
  const { student, setStudent } = useAuth()

  async function handleLogout() {
    await logout()
    setStudent(null)
    navigate('/login')
  }

  return (
    <header className="border-b border-zinc-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-4">
        <div className="flex items-center gap-4 sm:gap-6">
          <div className="flex items-center gap-2">
            <Logo className="h-8 w-8" />
            <span className="hidden text-lg font-semibold text-zinc-900 sm:inline">Handoff</span>
          </div>
          <nav className="flex items-center gap-4">
            <NavLink to="/feed" className={navLinkClass}>
              Marketplace
            </NavLink>
            <NavLink to="/profile" className={navLinkClass}>
              My profile
            </NavLink>
          </nav>
        </div>
        <div className="flex items-center gap-4">
          <span className="hidden text-sm text-zinc-600 sm:inline">Logged in as {student?.studentName}</span>
          <Button type="button" fullWidth={false} onClick={handleLogout} className="whitespace-nowrap px-4 py-2 text-sm">
            Log out
          </Button>
        </div>
      </div>
    </header>
  )
}
