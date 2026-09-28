import { CATEGORY_LABELS, CONDITION_LABELS } from '../lib/listingOptions'
import type { Listing } from '../services/listingService'

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
    <article className="flex flex-col gap-2 rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-base font-semibold text-zinc-900">{listing.itemName}</h2>
        <span className="text-base font-semibold text-accent-700">${listing.price.toFixed(2)}</span>
      </div>
      <div className="flex flex-wrap gap-1.5">
        <span className="rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
          {CATEGORY_LABELS[listing.category]}
        </span>
        <span className="rounded-full bg-zinc-100 px-2.5 py-0.5 text-xs font-medium text-zinc-700">
          {CONDITION_LABELS[listing.condition]}
        </span>
      </div>
      <p className="text-sm text-zinc-600">{listing.description}</p>
      <div className="mt-auto flex items-center justify-between gap-2">
        <p className="text-xs text-zinc-400">
          {listing.sellerName} · {formatPostedDate(listing.createdAt)}
        </p>
        {(onEdit || onDelete) && (
          <div className="flex items-center gap-3">
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
    </article>
  )
}
