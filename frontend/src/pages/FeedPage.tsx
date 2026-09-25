import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Logo } from '../components/Logo'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { ListingCard } from '../components/ListingCard'
import { ListingForm } from '../components/ListingForm'
import { useAuth } from '../lib/auth-context'
import { ApiError, logout } from '../services/authService'
import {
  createListing,
  deleteListing,
  getCategories,
  getListings,
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

export function FeedPage() {
  const navigate = useNavigate()
  const { student, setStudent } = useAuth()
  const [listings, setListings] = useState<Listing[]>([])
  const [categoryOptions, setCategoryOptions] = useState<Category[]>([])
  const [feedError, setFeedError] = useState<string | null>(null)
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
    getListings()
      .then(setListings)
      .catch((error: unknown) => {
        setFeedError(error instanceof ApiError ? error.message : 'Could not load the feed.')
      })

    getCategories()
      .then(setCategoryOptions)
      .catch(() => {
        // Category dropdown falls back to an empty list; the create/edit form
        // still works, it just has no options until this succeeds on retry.
      })
  }, [])

  async function handleLogout() {
    await logout()
    setStudent(null)
    navigate('/login')
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
        setListings((current) => current.map((listing) => (listing.id === updated.id ? updated : listing)))
      } else {
        const created = await createListing(input)
        setListings((current) => [created, ...current])
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
      .then(() => setListings((current) => current.filter((listing) => listing.id !== id)))
      .catch((error: unknown) => {
        setFeedError(error instanceof ApiError ? error.message : 'Could not delete the listing.')
      })
  }

  return (
    <div className="min-h-dvh bg-zinc-50">
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
          <div className="flex items-center gap-2">
            <Logo className="h-8 w-8" />
            <span className="text-lg font-semibold text-zinc-900">Handoff</span>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-zinc-600">Logged in as {student?.studentName}</span>
            <Button type="button" fullWidth={false} onClick={handleLogout} className="px-4 py-2 text-sm">
              Log out
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">
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

        {feedError && <FormAlert kind="error">{feedError}</FormAlert>}

        {listings.length === 0 && !feedError ? (
          <p className="text-sm text-zinc-500">No listings yet. Be the first to post one!</p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {listings.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                onEdit={listing.sellerId === student?.id ? () => openEditForm(listing) : undefined}
                onDelete={listing.sellerId === student?.id ? () => handleDelete(listing.id) : undefined}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
