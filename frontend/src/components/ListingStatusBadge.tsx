import type { ListingStatus } from '../services/listingService'

/** Pending or Sold pill. Available listings need no badge, so nothing renders for ACTIVE or a missing status. */
export function ListingStatusBadge({
  status,
}: {
  status?: ListingStatus | null
}) {
  if (status === 'PENDING') {
    return (
      <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-800">
        Pending
      </span>
    )
  }
  if (status === 'SOLD') {
    return (
      <span className="rounded-full bg-zinc-800 px-2.5 py-0.5 text-xs font-medium text-white">
        Sold
      </span>
    )
  }
  return null
}
