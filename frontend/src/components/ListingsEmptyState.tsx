import { MagnifyingGlass, Package } from '@phosphor-icons/react'
import { useSearchParams } from 'react-router-dom'
import { Button } from './ui/Button'

export function ListingsEmptyState() {
  const [searchParams, setSearchParams] = useSearchParams()
  // The page number alone isn't a filter, so it shouldn't offer "Clear Filters".
  const hasFilters = [...searchParams.keys()].some((key) => key !== 'page')
  const Icon = hasFilters ? MagnifyingGlass : Package

  return (
    <div className="rounded-2xl border border-dashed border-zinc-300 bg-white p-10 text-center">
      <Icon aria-hidden="true" className="mx-auto mb-4 h-10 w-10 text-accent-400" weight="duotone" />
      {hasFilters ? (
        <>
          <h2 className="text-lg font-semibold text-zinc-900">No listings match your filters</h2>
          <p className="mt-2 text-sm text-zinc-500">Try clearing your filters to see more listings.</p>
          <Button type="button" fullWidth={false} className="mt-5" onClick={() => setSearchParams({})}>
            Clear Filters
          </Button>
        </>
      ) : (
        <p className="text-sm text-zinc-500">No listings yet. Be the first to post one!</p>
      )}
    </div>
  )
}
