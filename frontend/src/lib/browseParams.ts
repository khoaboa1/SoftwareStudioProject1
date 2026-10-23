import { CATEGORY_OPTIONS } from './listingOptions'
import type { Category, SearchListingsParams } from '../services/listingService'

export type SortOption = 'newest' | 'price_asc' | 'price_desc'
export const PAGE_SIZE = 24

export const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
]

export type BrowseFilters = {
  q: string
  category: Category | null
  minPrice: number | null
  maxPrice: number | null
  sort: SortOption
  page: number
}

function parsePrice(value: string | null): number | null {
  if (value === null || !/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(value)) return null
  const price = Number(value)
  return Number.isFinite(price) ? price : null
}

export function parseBrowseParams(params: URLSearchParams): BrowseFilters {
  const category = CATEGORY_OPTIONS.find(
    (option) => option.value.toLowerCase() === params.get('category')?.toLowerCase(),
  )?.value ?? null
  const rawSort = params.get('sort')
  const sort = SORT_OPTIONS.find((option) => option.value === rawSort)?.value ?? 'newest'
  const rawPage = params.get('page')
  const page = rawPage !== null && /^[1-9]\d*$/.test(rawPage) && Number.isSafeInteger(Number(rawPage))
    ? Number(rawPage)
    : 1
  let minPrice = parsePrice(params.get('minPrice'))
  let maxPrice = parsePrice(params.get('maxPrice'))
  if (minPrice !== null && maxPrice !== null && minPrice > maxPrice) {
    minPrice = null
    maxPrice = null
  }
  return { q: (params.get('q') ?? '').trim(), category, minPrice, maxPrice, sort, page }
}

export function isBrowseActive(filters: BrowseFilters): boolean {
  return Boolean(filters.q || filters.category || filters.minPrice !== null || filters.maxPrice !== null || filters.sort !== 'newest')
}

export function toSearchParams(filters: BrowseFilters): SearchListingsParams {
  const { sortBy, sortOrder } = filters.sort === 'price_asc'
    ? { sortBy: 'price' as const, sortOrder: 'asc' as const }
    : filters.sort === 'price_desc'
      ? { sortBy: 'price' as const, sortOrder: 'desc' as const }
      : { sortBy: 'createdAt' as const, sortOrder: 'desc' as const }

  return {
    ...(filters.q ? { q: filters.q } : {}),
    ...(filters.category ? { category: filters.category } : {}),
    ...(filters.minPrice !== null ? { minPrice: filters.minPrice } : {}),
    ...(filters.maxPrice !== null ? { maxPrice: filters.maxPrice } : {}),
    sortBy,
    sortOrder,
    page: filters.page,
    limit: PAGE_SIZE,
  }
}
