import { NavLink, useNavigate } from 'react-router-dom'
import { Logo } from './Logo'
import { MessagesMenu } from './MessagesMenu'
import { Button } from './ui/Button'
import { useAuth } from '../lib/auth-context'
import { createAuthFacade } from '../services/authFacade'
import { isAdmin } from '../lib/roles'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getOpenReports } from '../services/adminService'
import { CountBadge } from './ui/CountBadge'

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `whitespace-nowrap text-sm font-medium ${isActive ? 'text-accent-700' : 'text-zinc-600 hover:text-zinc-900'}`
}

type AppHeaderProps = {
  /**
   * Live open-report count from a page that already loads reports (the admin page). When omitted, an admin's
   * header loads the count itself; `null` means "still loading, don't fetch".
   */
  openReportCount?: number | null
}

export function AppHeader({ openReportCount }: AppHeaderProps = {}) {
  const navigate = useNavigate()
  const { student, setStudent } = useAuth()
  const admin = isAdmin(student)
  const ownCount = useAsyncResource(getOpenReports, [], {
    enabled: admin && openReportCount === undefined,
  })
  const reportCount =
    openReportCount !== undefined
      ? openReportCount
      : ownCount.state.status === 'ready'
        ? ownCount.state.data.length
        : null
  const auth = createAuthFacade(setStudent)

  async function handleLogout() {
    await auth.signOut()
    navigate('/login')
  }

  return (
    <header className="border-b border-zinc-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-4">
        <div className="flex items-center gap-4 sm:gap-6">
          <div className="flex items-center gap-2">
            <Logo className="h-8 w-8" />
            <span className="hidden text-lg font-semibold text-zinc-900 sm:inline">
              Handoff
            </span>
          </div>
          <nav className="flex items-center gap-4">
            <NavLink to="/feed" className={navLinkClass}>
              Marketplace
            </NavLink>
            <NavLink to="/profile" className={navLinkClass}>
              My profile
            </NavLink>
            {admin && (
              <NavLink to="/admin" className={navLinkClass}>
                Admin
                <CountBadge count={reportCount} noun="open reports" />
              </NavLink>
            )}
          </nav>
        </div>
        <div className="flex items-center gap-4">
          <MessagesMenu />
          <span className="hidden text-sm text-zinc-600 sm:inline">
            Logged in as {student?.studentName}
          </span>
          <Button
            type="button"
            fullWidth={false}
            onClick={handleLogout}
            className="whitespace-nowrap px-4 py-2 text-sm"
          >
            Log out
          </Button>
        </div>
      </div>
    </header>
  )
}
