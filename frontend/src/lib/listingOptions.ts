import type { Category, Condition } from '../services/listingService'

export const CONDITION_OPTIONS: { value: Condition; label: string }[] = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'WORN', label: 'Worn' },
]

export const CATEGORY_OPTIONS: { value: Category; label: string }[] = [
  { value: 'FURNITURE', label: 'Furniture' },
  { value: 'ELECTRONICS', label: 'Electronics' },
  { value: 'KITCHEN', label: 'Kitchen' },
  { value: 'DECOR', label: 'Decor' },
  { value: 'CLOTHING', label: 'Clothing' },
  { value: 'OTHER', label: 'Other' },
]

export const CONDITION_LABELS = Object.fromEntries(
  CONDITION_OPTIONS.map((option) => [option.value, option.label]),
) as Record<Condition, string>

export const CATEGORY_LABELS = Object.fromEntries(
  CATEGORY_OPTIONS.map((option) => [option.value, option.label]),
) as Record<Category, string>
