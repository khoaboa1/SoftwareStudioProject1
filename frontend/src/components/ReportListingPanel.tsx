import { useState, type FormEvent } from 'react'
import { Button } from './ui/Button'
import { FormAlert } from './ui/FormAlert'
import { getErrorMessage } from '../lib/apiError'
import {
  reportListing,
  REPORT_REASON_LABELS,
  type ReportReason,
} from '../services/reportService'

export function ReportListingPanel({
  listingId,
  onDone,
  onCancel,
}: {
  listingId: number
  onDone: () => void
  onCancel: () => void
}) {
  const [reason, setReason] = useState<ReportReason | null>(null)
  const [details, setDetails] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!reason || loading) return
    setLoading(true)
    setError(null)
    try {
      await reportListing(listingId, {
        reason,
        details: details.trim() || undefined,
      })
      onDone()
    } catch (cause) {
      setError(getErrorMessage(cause, 'Could not send report.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={submit} className="mt-5 border-l-2 border-accent-700 pl-4">
      <fieldset>
        <legend className="mb-3 text-sm font-semibold text-zinc-900">
          Why are you reporting this listing?
        </legend>
        <div className="grid gap-2">
          {(
            Object.entries(REPORT_REASON_LABELS) as [ReportReason, string][]
          ).map(([value, label]) => (
            <label
              key={value}
              className="flex items-center gap-2 text-sm text-zinc-700"
            >
              <input
                type="radio"
                name="reason"
                value={value}
                checked={reason === value}
                onChange={() => setReason(value)}
              />
              {label}
            </label>
          ))}
        </div>
      </fieldset>
      <label
        htmlFor="report-details"
        className="mt-4 block text-sm font-medium text-zinc-700"
      >
        Details (optional)
      </label>
      <textarea
        id="report-details"
        maxLength={500}
        value={details}
        onChange={(event) => setDetails(event.target.value)}
        className="mt-1 w-full rounded-lg border border-zinc-200 bg-white p-3 text-sm text-zinc-900 focus:border-accent-500"
      />
      <p className="mb-3 text-right text-xs text-zinc-500">
        {details.length}/500
      </p>
      {error && <FormAlert kind="error">{error}</FormAlert>}
      <div className="flex flex-wrap gap-3">
        <Button
          type="submit"
          fullWidth={false}
          disabled={!reason}
          loading={loading}
        >
          Send report
        </Button>
        <button
          type="button"
          disabled={loading}
          onClick={onCancel}
          className="text-sm font-medium text-zinc-600 hover:text-zinc-900"
        >
          Cancel
        </button>
      </div>
    </form>
  )
}
