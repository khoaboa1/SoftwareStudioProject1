import { useState } from 'react'
import { AppHeader } from '../components/AppHeader'
import { CountBadge } from '../components/ui/CountBadge'
import { useSearchParams } from 'react-router-dom'
import { ReportsTab } from '../components/admin/ReportsTab'
import { StudentsTab } from '../components/admin/StudentsTab'

export function AdminPage() {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'students' ? 'students' : 'reports'
  // Reports load once here; the tab badge and the header badge share the live count.
  const [reportCount, setReportCount] = useState<number | null>(null)
  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader openReportCount={reportCount} />
      <main className="mx-auto max-w-5xl px-4 py-8">
        <h1 className="text-2xl font-semibold text-zinc-900">Admin</h1>
        <div
          role="tablist"
          aria-label="Admin sections"
          className="mt-6 flex gap-6 border-b border-zinc-200"
        >
          {(['reports', 'students'] as const).map((name) => (
            <button
              key={name}
              id={`tab-${name}`}
              type="button"
              role="tab"
              aria-controls={`panel-${name}`}
              aria-selected={tab === name}
              onClick={() => setParams({ tab: name })}
              className={`border-b-2 pb-3 text-sm font-medium ${tab === name ? 'border-accent-700 text-accent-700' : 'border-transparent text-zinc-600 hover:text-zinc-900'}`}
            >
              {name === 'reports' ? (
                <>
                  Reports
                  <CountBadge count={reportCount} noun="open reports" />
                </>
              ) : (
                'Students'
              )}
            </button>
          ))}
        </div>
        {/* The Reports panel stays mounted (hidden) so its count stays current on the Students tab. */}
        <section
          id="panel-reports"
          role="tabpanel"
          aria-labelledby="tab-reports"
          hidden={tab !== 'reports'}
          className="mt-6"
        >
          <ReportsTab onCountChange={setReportCount} />
        </section>
        {tab === 'students' && (
          <section
            id="panel-students"
            role="tabpanel"
            aria-labelledby="tab-students"
            className="mt-6"
          >
            <StudentsTab />
          </section>
        )}
      </main>
    </div>
  )
}
