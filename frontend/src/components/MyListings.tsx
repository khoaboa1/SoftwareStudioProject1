import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ListingCard } from './ListingCard'
import { FormAlert } from './ui/FormAlert'
import { ApiError } from '../services/httpClient'
import { getListings, type Listing } from '../services/listingService'

type ListingsState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; listings: Listing[] }

/**
 * There's no per-seller endpoint, so this filters the school feed. The feed only
 * includes sellers from the requester's own school, which always includes the
 * requester, so none of their listings can be missing.
 */
export function MyListings({ sellerId }: { sellerId: number }) {
  const [state, setState] = useState<ListingsState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    getListings()
      .then((listings) => {
        if (!cancelled) {
          setState({ status: 'ready', listings: listings.filter((listing) => listing.sellerId === sellerId) })
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setState({
            status: 'error',
            error: error instanceof ApiError ? error.message : 'Could not load your listings.',
          })
        }
      })
    return () => {
      cancelled = true
    }
  }, [sellerId])

  return (
    <section className="mt-8">
      <h2 className="mb-4 text-lg font-semibold text-zinc-900">
        {state.status === 'ready' ? `My listings (${state.listings.length})` : 'My listings'}
      </h2>

      {state.status === 'loading' && <p className="text-sm text-zinc-500">Loading your listings…</p>}

      {state.status === 'error' && <FormAlert kind="error">{state.error}</FormAlert>}

      {state.status === 'ready' && state.listings.length === 0 && (
        <p className="text-sm text-zinc-500">
          You haven't posted any listings yet.{' '}
          <Link to="/feed" className="font-medium text-accent-700 hover:text-accent-800">
            Post one in the marketplace
          </Link>
        </p>
      )}

      {state.status === 'ready' && state.listings.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {state.listings.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </div>
      )}
    </section>
  )
}
