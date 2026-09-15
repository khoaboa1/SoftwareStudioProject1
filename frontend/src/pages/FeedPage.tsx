import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Logo } from '../components/Logo'
import { Button } from '../components/ui/Button'
import { FormAlert } from '../components/ui/FormAlert'
import { TextField } from '../components/ui/TextField'
import { SelectField } from '../components/ui/SelectField'
import { useAuth } from '../lib/auth-context'
import { ApiError, logout } from '../services/authService'
import {
  createListing,
  getListings,
  type Category,
  type Condition,
  type Listing,
} from '../services/listingService'

const CONDITION_OPTIONS: { value: Condition; label: string }[] = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'WORN', label: 'Worn' },
]

const CATEGORY_OPTIONS: { value: Category; label: string }[] = [
  { value: 'FURNITURE', label: 'Furniture' },
  { value: 'ELECTRONICS', label: 'Electronics' },
  { value: 'KITCHEN', label: 'Kitchen' },
  { value: 'DECOR', label: 'Decor' },
  { value: 'CLOTHING', label: 'Clothing' },
  { value: 'OTHER', label: 'Other' },
]

const CONDITION_LABELS = Object.fromEntries(
  CONDITION_OPTIONS.map((option) => [option.value, option.label]),
) as Record<Condition, string>

const CATEGORY_LABELS = Object.fromEntries(
  CATEGORY_OPTIONS.map((option) => [option.value, option.label]),
) as Record<Category, string>

function formatPostedDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  })
}

export function FeedPage() {
  const navigate = useNavigate()
  const { student, setStudent } = useAuth()
  const [listings, setListings] = useState<Listing[]>([])
  const [feedError, setFeedError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

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
  }, [])

  async function handleLogout() {
    await logout()
    setStudent(null)
    navigate('/login')
  }

  async function handleCreateListing(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const parsedPrice = Number(price)
    if (!itemName.trim() || !description.trim() || !Number.isFinite(parsedPrice) || parsedPrice <= 0) {
      setFormError('Fill in an item name, description, and a price greater than $0.')
      setFormStatus('error')
      return
    }

    setFormStatus('submitting')
    setFormError(null)

    try {
      const listing = await createListing({
        itemName,
        description,
        price: parsedPrice,
        condition,
        category,
      })
      setListings((current) => [listing, ...current])
      setItemName('')
      setDescription('')
      setPrice('')
      setCondition('GOOD')
      setCategory('OTHER')
      setShowForm(false)
      setFormStatus('idle')
    } catch (error) {
      setFormError(error instanceof ApiError ? error.message : 'Something went wrong. Please try again.')
      setFormStatus('error')
    }
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
            onClick={() => setShowForm((current) => !current)}
            className="px-4 py-2 text-sm"
          >
            {showForm ? 'Cancel' : 'Post a listing'}
          </Button>
        </div>

        {showForm && (
          <form
            onSubmit={handleCreateListing}
            className="mb-8 flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm"
          >
            {formStatus === 'error' && formError && <FormAlert kind="error">{formError}</FormAlert>}

            <TextField
              label="Item name"
              placeholder="Desk Lamp"
              value={itemName}
              onChange={(e) => setItemName(e.target.value)}
            />

            <div className="flex flex-col gap-1.5">
              <label htmlFor="listing-description" className="text-sm font-medium text-zinc-800">
                Description
              </label>
              <textarea
                id="listing-description"
                rows={3}
                placeholder="Barely used, works great"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-lg border border-zinc-300 px-3.5 py-2.5 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
              />
            </div>

            <TextField
              label="Price (USD)"
              type="number"
              min="0"
              step="0.01"
              placeholder="10.00"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />

            <SelectField
              label="Condition"
              options={CONDITION_OPTIONS}
              value={condition}
              onChange={(e) => setCondition(e.target.value as Condition)}
            />

            <SelectField
              label="Category"
              options={CATEGORY_OPTIONS}
              value={category}
              onChange={(e) => setCategory(e.target.value as Category)}
            />

            <Button type="submit" loading={formStatus === 'submitting'}>
              Post listing
            </Button>
          </form>
        )}

        {feedError && <FormAlert kind="error">{feedError}</FormAlert>}

        {listings.length === 0 && !feedError ? (
          <p className="text-sm text-zinc-500">No listings yet. Be the first to post one!</p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {listings.map((listing) => (
              <article
                key={listing.id}
                className="flex flex-col gap-2 rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-2">
                  <h2 className="text-base font-semibold text-zinc-900">{listing.itemName}</h2>
                  <span className="text-base font-semibold text-accent-700">
                    ${listing.price.toFixed(2)}
                  </span>
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
                <p className="mt-auto text-xs text-zinc-400">
                  {listing.sellerName} · {formatPostedDate(listing.createdAt)}
                </p>
              </article>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
