import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { ListingThumbnail } from '../components/ListingThumbnail'
import { FormAlert } from '../components/ui/FormAlert'
import { CATEGORY_LABELS, CONDITION_LABELS } from '../lib/listingOptions'
import { ApiError } from '../services/httpClient'
import { getListing, type Listing } from '../services/listingService'

type DetailState =
  | { status: 'loading' }
  | { status: 'notFound' }
  | { status: 'error'; error: string }
  | { status: 'ready'; listing: Listing }

export function ListingDetailPage() {
  const { id } = useParams()
  const listingId = Number(id)
  const validId = Number.isInteger(listingId) && listingId > 0
  const [state, setState] = useState<DetailState>(validId ? { status: 'loading' } : { status: 'notFound' })

  useEffect(() => {
    if (!validId) return
    let cancelled = false
    getListing(listingId)
      .then((listing) => {
        if (!cancelled) setState({ status: 'ready', listing })
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setState(error instanceof ApiError && error.status === 404
            ? { status: 'notFound' }
            : { status: 'error', error: error instanceof ApiError ? error.message : 'Could not load this listing.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [listingId, validId])

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />
      <main className="mx-auto max-w-4xl px-4 py-8">
        <Link to="/feed" className="mb-6 inline-block text-sm font-medium text-accent-700 hover:text-accent-800">
          Back to marketplace
        </Link>

        {state.status === 'loading' && (
          <div role="status" className="grid gap-6 md:grid-cols-2">
            <span className="sr-only">Loading listing</span>
            <div className="aspect-[4/3] animate-pulse rounded-2xl bg-zinc-200" />
            <div className="space-y-4">
              <div className="h-8 w-3/4 animate-pulse rounded bg-zinc-200" />
              <div className="h-6 w-1/3 animate-pulse rounded bg-zinc-200" />
              <div className="h-24 animate-pulse rounded bg-zinc-200" />
            </div>
          </div>
        )}

        {state.status === 'notFound' && <h1 className="text-2xl font-semibold text-zinc-900">Listing not found</h1>}
        {state.status === 'error' && <FormAlert kind="error">{state.error}</FormAlert>}

        {state.status === 'ready' && (
          <div className="grid gap-6 md:grid-cols-2">
            <ListingThumbnail category={state.listing.category} className="overflow-hidden rounded-2xl" />
            <div>
              <h1 className="text-2xl font-semibold text-zinc-900">{state.listing.itemName}</h1>
              <p className="mt-2 text-xl font-semibold text-accent-700">${state.listing.price.toFixed(2)}</p>
              <div className="mt-4 flex flex-wrap gap-1.5">
                <span className="rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-medium text-accent-800">
                  {CATEGORY_LABELS[state.listing.category]}
                </span>
                <span className="rounded-full bg-zinc-100 px-2.5 py-0.5 text-xs font-medium text-zinc-700">
                  {CONDITION_LABELS[state.listing.condition]}
                </span>
              </div>
              <p className="mt-6 whitespace-pre-line text-sm text-zinc-700">{state.listing.description}</p>
              <p className="mt-6 text-sm text-zinc-500">Sold by {state.listing.sellerName}</p>
              <p className="mt-1 text-sm text-zinc-500">
                Posted {new Date(state.listing.createdAt).toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' })}
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
