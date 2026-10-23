import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { ListingCard } from '../components/ListingCard'
import { ListingGrid } from '../components/ListingGrid'
import { ListingGridSkeleton } from '../components/ListingCardSkeleton'
import { ListingsEmptyState } from '../components/ListingsEmptyState'
import { ListingForm } from '../components/ListingForm'
import { SearchBar } from '../components/SearchBar'
import { FilterBar } from '../components/FilterBar'
import { isBrowseActive, parseBrowseParams, toSearchParams } from '../lib/browseParams'
import { useAuth } from '../lib/auth-context'
import { ApiError } from '../services/authService'
import {
  createListing,
  deleteListing,
  getCategories,
  getListings,
  searchListings,
  updateListing,
  type Category,
  type Condition,
  type Listing,
} from '../services/listingService'

function resetFormDefaults() {
  return {
    itemName: '',
    description: '',
    price: '',
    condition: 'GOOD' as Condition,
    category: 'OTHER' as Category,
  }
}

type FeedState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; listings: Listing[] }

export function FeedPage() {
  const { student } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = useMemo(() => parseBrowseParams(searchParams), [searchParams])
  const filtersKey = JSON.stringify(filters)
  const [feedState, setFeedState] = useState<FeedState>({ status: 'loading' })
  const [clearVersion, setClearVersion] = useState(0)
  const [categoryOptions, setCategoryOptions] = useState<Category[]>([])
  const [actionError, setActionError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingListingId, setEditingListingId] = useState<number | null>(null)

  const [itemName, setItemName] = useState('')
  const [description, setDescription] = useState('')
  const [price, setPrice] = useState('')
  const [condition, setCondition] = useState<Condition>('GOOD')
  const [category, setCategory] = useState<Category>('OTHER')
  const [formStatus, setFormStatus] = useState<'idle' | 'submitting' | 'error'>('idle')
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    getCategories()
      .then(setCategoryOptions)
      .catch(() => {
        // Category dropdown falls back to an empty list; the create/edit form
        // still works, it just has no options until this succeeds on retry.
      })
  }, [])

  useEffect(() => {
    let cancelled = false
    // oxlint-disable-next-line react/set-state-in-effect -- New filters must show the loading skeleton immediately.
    setFeedState({ status: 'loading' })
    const listingsRequest = isBrowseActive(filters)
      ? searchListings(toSearchParams(filters)).then((page) => page.items)
      : getListings()

    listingsRequest
      .then((listings) => {
        if (!cancelled) setFeedState({ status: 'ready', listings })
      })
      .catch((error: unknown) => {
        if (!cancelled) setFeedState({ status: 'error', error: error instanceof ApiError ? error.message : 'Could not load the feed.' })
      })

    return () => { cancelled = true }
  // oxlint-disable-next-line react-hooks/exhaustive-deps -- The serialized parsed filters are the request key.
  }, [filtersKey])

  function updateParams(patch: Record<string, string | null>) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      for (const [key, value] of Object.entries(patch)) {
        if (value === null) next.delete(key)
        else next.set(key, value)
      }
      return next
    }, { replace: true })
  }

  function handleSearch(term: string) {
    updateParams({ q: term || null })
  }

  function handleClearAll() {
    setClearVersion((version) => version + 1)
    setSearchParams({}, { replace: true })
  }

  function toggleCreateForm() {
    if (showForm) {
      setShowForm(false)
      setEditingListingId(null)
      setFormError(null)
      setFormStatus('idle')
      return
    }

    setEditingListingId(null)
    const defaults = resetFormDefaults()
    setItemName(defaults.itemName)
    setDescription(defaults.description)
    setPrice(defaults.price)
    setCondition(defaults.condition)
    setCategory(defaults.category)
    setFormError(null)
    setFormStatus('idle')
    setShowForm(true)
  }

  function openEditForm(listing: Listing) {
    setEditingListingId(listing.id)
    setItemName(listing.itemName)
    setDescription(listing.description)
    setPrice(String(listing.price))
    setCondition(listing.condition)
    setCategory(listing.category)
    setFormError(null)
    setFormStatus('idle')
    setShowForm(true)
  }

  async function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const parsedPrice = Number(price)
    if (!itemName.trim() || !description.trim() || !Number.isFinite(parsedPrice) || parsedPrice <= 0) {
      setFormError('Fill in an item name, description, and a price greater than $0.')
      setFormStatus('error')
      return
    }

    setFormStatus('submitting')
    setFormError(null)

    const input = { itemName, description, price: parsedPrice, condition, category }

    try {
      if (editingListingId !== null) {
        const updated = await updateListing(editingListingId, input)
        setFeedState((current) => current.status === 'ready'
          ? { status: 'ready', listings: current.listings.map((listing) => (listing.id === updated.id ? updated : listing)) }
          : current)
      } else {
        const created = await createListing(input)
        setFeedState((current) => current.status === 'ready'
          ? { status: 'ready', listings: [created, ...current.listings] }
          : current)
      }
      setShowForm(false)
      setEditingListingId(null)
      const defaults = resetFormDefaults()
      setItemName(defaults.itemName)
      setDescription(defaults.description)
      setPrice(defaults.price)
      setCondition(defaults.condition)
      setCategory(defaults.category)
      setFormStatus('idle')
    } catch (error) {
      setFormError(error instanceof ApiError ? error.message : 'Something went wrong. Please try again.')
      setFormStatus('error')
    }
  }

  function handleDelete(id: number) {
    deleteListing(id)
      .then(() => setFeedState((current) => current.status === 'ready'
        ? { status: 'ready', listings: current.listings.filter((listing) => listing.id !== id) }
        : current))
      .catch((error: unknown) => {
        setActionError(error instanceof ApiError ? error.message : 'Could not delete the listing.')
      })
  }

  return (
    <div className="min-h-dvh bg-zinc-50">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-4 py-8">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-xl font-semibold text-zinc-900">Campus Marketplace</h1>
          <Button
            type="button"
            fullWidth={false}
            onClick={toggleCreateForm}
            className="px-4 py-2 text-sm"
          >
            {showForm ? 'Cancel' : 'Post a listing'}
          </Button>
        </div>

        <SearchBar key={`search-${clearVersion}`} value={filters.q} onSearch={handleSearch} />
        <FilterBar key={`filter-${clearVersion}`} filters={filters} onChange={updateParams} onClear={handleClearAll} />

        {showForm && (
          <ListingForm
            itemName={itemName}
            onItemNameChange={setItemName}
            description={description}
            onDescriptionChange={setDescription}
            price={price}
            onPriceChange={setPrice}
            condition={condition}
            onConditionChange={setCondition}
            category={category}
            onCategoryChange={setCategory}
            categoryOptions={categoryOptions}
            status={formStatus}
            error={formError}
            submitLabel={editingListingId !== null ? 'Save changes' : 'Post listing'}
            onSubmit={handleFormSubmit}
          />
        )}

        {actionError && <FormAlert kind="error">{actionError}</FormAlert>}

        {feedState.status === 'loading' && <ListingGridSkeleton />}
        {feedState.status === 'error' && <FormAlert kind="error">{feedState.error}</FormAlert>}
        {feedState.status === 'ready' && feedState.listings.length === 0 && <ListingsEmptyState />}
        {feedState.status === 'ready' && feedState.listings.length > 0 && (
          <ListingGrid>
            {feedState.listings.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                onEdit={listing.sellerId === student?.id ? () => openEditForm(listing) : undefined}
                onDelete={listing.sellerId === student?.id ? () => handleDelete(listing.id) : undefined}
              />
            ))}
          </ListingGrid>
        )}
      </main>
    </div>
  )
}
