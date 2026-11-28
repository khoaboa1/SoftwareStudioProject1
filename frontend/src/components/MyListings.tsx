import { Link } from 'react-router-dom'
import { ListingCard } from './ListingCard'
import { ListingGrid } from './ListingGrid'
import { ListingGridSkeleton } from './ListingCardSkeleton'
import { FormAlert } from './ui/FormAlert'
import { getErrorMessage } from '../lib/apiError'
import { useAsyncResource } from '../lib/useAsyncResource'
import { getListings } from '../services/listingService'

/**
 * There's no per-seller endpoint, so this filters the school feed. The feed only
 * includes sellers from the requester's own school, which always includes the
 * requester, so none of their listings can be missing.
 * When other students' profiles pass ownerName, SOLD listings are hidden.
 */
export function MyListings({
  sellerId,
  ownerName,
}: {
  sellerId: number
  ownerName?: string
}) {
  const { state } = useAsyncResource(
    () =>
      getListings().then((listings) =>
        listings.filter(
          (listing) =>
            listing.sellerId === sellerId &&
            (ownerName === undefined || listing.status !== 'SOLD')
        )
      ),
    [sellerId, ownerName]
  )

  const title = ownerName ? `${ownerName}'s listings` : 'My listings'
  return (
    <section className="mt-8">
      <h2 className="mb-4 text-lg font-semibold text-zinc-900">
        {state.status === 'ready' ? `${title} (${state.data.length})` : title}
      </h2>

      {state.status === 'loading' && <ListingGridSkeleton count={2} />}

      {state.status === 'error' && (
        <FormAlert kind="error">
          {getErrorMessage(
            state.error,
            ownerName
              ? 'Could not load listings.'
              : 'Could not load your listings.'
          )}
        </FormAlert>
      )}

      {state.status === 'ready' &&
        state.data.length === 0 &&
        (ownerName ? (
          <p className="text-sm text-zinc-500">
            {ownerName} has no listings right now.
          </p>
        ) : (
          <p className="text-sm text-zinc-500">
            You haven't posted any listings yet.{' '}
            <Link
              to="/feed"
              className="font-medium text-accent-700 hover:text-accent-800"
            >
              Post one in the marketplace
            </Link>
          </p>
        ))}

      {state.status === 'ready' && state.data.length > 0 && (
        <ListingGrid>
          {state.data.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </ListingGrid>
      )}
    </section>
  )
}
