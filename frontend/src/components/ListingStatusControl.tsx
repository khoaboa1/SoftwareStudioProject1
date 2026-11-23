import { useId, useState } from 'react'
import { FormAlert } from './ui/FormAlert'
import { getErrorMessage } from '../lib/apiError'
import { STATUS_OPTIONS } from '../lib/listingOptions'
import {
  updateListingStatus,
  type Listing,
  type ListingStatus,
} from '../services/listingService'

type ListingStatusControlProps = {
  listingId: number
  status: ListingStatus
  onChange: (listing: Listing) => void
}

/**
 * The seller's Available / Pending / Sold switch. The checked option always follows `status`, so a failed save
 * leaves the old one selected.
 */
export function ListingStatusControl({
  listingId,
  status,
  onChange,
}: ListingStatusControlProps) {
  const labelId = useId()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function choose(next: ListingStatus) {
    if (next === status || saving) return
    setError(null)
    setSaving(true)
    try {
      onChange(await updateListingStatus(listingId, next))
    } catch (err) {
      setError(getErrorMessage(err, 'Could not update the status.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mt-6">
      <p id={labelId} className="mb-2 text-sm font-medium text-zinc-700">
        Listing status
      </p>
      <div
        role="radiogroup"
        aria-labelledby={labelId}
        className="inline-flex rounded-lg border border-zinc-200 bg-white p-1"
      >
        {STATUS_OPTIONS.map((option) => (
          <label key={option.value} className="cursor-pointer">
            <input
              type="radio"
              name={`listing-status-${listingId}`}
              value={option.value}
              checked={status === option.value}
              disabled={saving}
              onChange={() => choose(option.value)}
              className="peer sr-only"
            />
            <span className="block rounded-md px-3 py-1.5 text-sm font-medium text-zinc-600 peer-checked:bg-accent-700 peer-checked:text-white peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-accent-500 peer-disabled:cursor-wait peer-disabled:opacity-60">
              {option.label}
            </span>
          </label>
        ))}
      </div>
      {error && (
        <div className="mt-3">
          <FormAlert kind="error">{error}</FormAlert>
        </div>
      )}
    </div>
  )
}
