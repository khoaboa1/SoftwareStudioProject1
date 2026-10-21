import { ListingGrid } from './ListingGrid'

export function ListingGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div role="status">
      <span className="sr-only">Loading listings</span>
      <ListingGrid>
        {Array.from({ length: count }, (_, index) => (
          <div
            key={index}
            aria-hidden="true"
            data-testid="listing-card-skeleton"
            className="overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-sm"
          >
            <div className="aspect-[4/3] animate-pulse bg-zinc-200" />
            <div className="space-y-3 p-4">
              <div className="h-5 w-3/4 animate-pulse rounded bg-zinc-200" />
              <div className="h-4 w-1/2 animate-pulse rounded bg-zinc-200" />
              <div className="h-4 w-2/3 animate-pulse rounded bg-zinc-200" />
            </div>
          </div>
        ))}
      </ListingGrid>
    </div>
  )
}
