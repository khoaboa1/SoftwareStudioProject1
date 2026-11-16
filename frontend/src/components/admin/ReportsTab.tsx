import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ConfirmInline } from '../ConfirmInline'
import { FormAlert } from '../ui/FormAlert'
import { Button } from '../ui/Button'
import { getErrorMessage } from '../../lib/apiError'
import { useAsyncResource } from '../../lib/useAsyncResource'
import {
  dismissReport,
  getOpenReports,
  type AdminReport,
} from '../../services/adminService'
import { deleteListing } from '../../services/listingService'
import { REPORT_REASON_LABELS } from '../../services/reportService'

function ReportRow({
  report,
  onDismiss,
  onDelete,
}: {
  report: AdminReport
  onDismiss: (id: number) => void
  onDelete: (listingId: number) => void
}) {
  const [confirming, setConfirming] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function dismiss() {
    if (loading) return
    setLoading(true)
    setError(null)
    try {
      await dismissReport(report.id)
      onDismiss(report.id)
    } catch (cause) {
      setError(getErrorMessage(cause, 'Could not dismiss report.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <article
      data-testid={`report-${report.id}`}
      className="border-b border-zinc-200 py-5"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link
            to={`/listings/${report.listingId}`}
            className="font-semibold text-accent-700 hover:text-accent-800"
          >
            {report.itemName}
          </Link>
          <p className="mt-1 text-sm font-medium text-zinc-800">
            {REPORT_REASON_LABELS[report.reason]}
          </p>
          {report.details && (
            <p className="mt-1 whitespace-pre-line text-sm text-zinc-700">
              {report.details}
            </p>
          )}
          <p className="mt-2 text-sm text-zinc-600">
            Reported by {report.reporterName}
          </p>
          <p className="text-sm text-zinc-600">
            Seller: {report.sellerName}{' '}
            {report.sellerSuspended && (
              <span className="ml-1 bg-zinc-200 px-2 py-0.5 text-xs">
                Suspended
              </span>
            )}
          </p>
          <time className="text-xs text-zinc-500" dateTime={report.createdAt}>
            {new Date(report.createdAt).toLocaleDateString()}
          </time>
        </div>
      </div>
      {error && <FormAlert kind="error">{error}</FormAlert>}
      {confirming ? (
        <ConfirmInline
          message="Delete this listing for everyone?"
          confirmLabel="Delete"
          onConfirm={async () => {
            await deleteListing(report.listingId)
            onDelete(report.listingId)
          }}
          onCancel={() => setConfirming(false)}
        />
      ) : (
        <div className="mt-3 flex flex-wrap gap-4">
          <button
            type="button"
            disabled={loading}
            onClick={() => setConfirming(true)}
            className="text-sm font-medium text-accent-700"
          >
            Delete listing
          </button>
          <Button
            type="button"
            fullWidth={false}
            loading={loading}
            onClick={dismiss}
            className="!bg-transparent !px-0 !py-0 !text-sm !text-zinc-700"
          >
            Dismiss
          </Button>
        </div>
      )}
    </article>
  )
}

export function ReportsTab({
  onCountChange,
}: {
  /** Called with the number of open reports whenever the loaded list changes. */
  onCountChange?: (count: number) => void
} = {}) {
  const { state, setData, reload } = useAsyncResource(getOpenReports, [])
  const openCount = state.status === 'ready' ? state.data.length : null
  useEffect(() => {
    if (openCount !== null) onCountChange?.(openCount)
  }, [openCount, onCountChange])
  if (state.status === 'loading' || state.status === 'idle')
    return (
      <p role="status" className="text-sm text-zinc-600">
        Loading reports…
      </p>
    )
  if (state.status === 'error')
    return (
      <FormAlert kind="error" action={{ label: 'Try again', onClick: reload }}>
        {getErrorMessage(state.error, 'Could not load reports.')}
      </FormAlert>
    )
  if (!state.data.length)
    return <p className="text-sm text-zinc-600">No open reports.</p>
  return (
    <div>
      {state.data.map((report) => (
        <ReportRow
          key={report.id}
          report={report}
          onDismiss={(id) =>
            setData((current) => current.filter((item) => item.id !== id))
          }
          onDelete={(listingId) =>
            setData((current) =>
              current.filter((item) => item.listingId !== listingId)
            )
          }
        />
      ))}
    </div>
  )
}
