import { Link } from 'react-router-dom'
import { CATEGORY_LABELS, CONDITION_LABELS } from '../lib/listingOptions'
import type { Listing } from '../services/listingService'
import { ListingThumbnail } from './ListingThumbnail'

function formatPostedDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  })
}

type ListingCardProps = {
  listing: Listing
  onEdit?: () => void
  onDelete?: () => void
}

export function ListingCard({ listing, onEdit, onDelete }: ListingCardProps) {
  return (
    <article className="relative flex flex-col overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-sm transition hover:shadow-md focus-within:ring-2 focus-within:ring-accent-300">
      <ListingThumbnail category={listing.category} />
      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-base font-semibold text-zinc-900">
            <Link to={`/listings/${listing.id}`} className="after:absolute after:inset-0">
              {listing.itemName}
            </Link>
          </h2>
          <span className="shrink-0 text-base font-semibold text-accent-700">${listing.price.toFixed(2)}</span>
        </div>
        <div className="flex flex-wrap gap-1.5">
          <span className="rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
            {CATEGORY_LABELS[listing.category]}
          </span>
          <span className="rounded-full bg-zinc-100 px-2.5 py-0.5 text-xs font-medium text-zinc-700">
            {CONDITION_LABELS[listing.condition]}
          </span>
        </div>
        <div className="mt-auto flex items-center justify-between gap-2">
          <p className="text-xs text-zinc-400">
            {listing.sellerName} · {formatPostedDate(listing.createdAt)}
          </p>
          {(onEdit || onDelete) && (
            <div className="relative z-10 flex items-center gap-3">
              {onEdit && (
                <button
                  type="button"
                  onClick={onEdit}
                  className="text-xs font-medium text-zinc-600 hover:text-zinc-800"
                >
                  Edit
                </button>
              )}
              {onDelete && (
                <button
                  type="button"
                  onClick={onDelete}
                  className="text-xs font-medium text-red-600 hover:text-red-700"
                >
                  Delete
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </article>
  )
}
