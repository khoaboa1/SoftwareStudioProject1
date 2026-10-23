import { request } from './httpClient'

export type Condition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'WORN'
export type Category = 'FURNITURE' | 'ELECTRONICS' | 'KITCHEN' | 'DECOR' | 'CLOTHING' | 'OTHER'

export type Listing = {
  id: number
  itemName: string
  description: string
  price: number
  condition: Condition
  category: Category
  sellerId: number
  sellerName: string
  createdAt: string
}

export type ListingPage = {
  items: Listing[]
  meta: {
    totalItems: number
    currentPage: number
    totalPages: number
    pageSize: number
  }
}

export type SearchListingsParams = {
  q?: string
  category?: Category
  minPrice?: number
  maxPrice?: number
  sortBy?: 'createdAt' | 'price' | 'itemName'
  sortOrder?: 'asc' | 'desc'
  page?: number
  limit?: number
}

export function searchListings(params: SearchListingsParams): Promise<ListingPage> {
  const query = new URLSearchParams()
  if (params.q !== undefined) query.set('q', params.q)
  if (params.category !== undefined) query.set('category', params.category)
  if (params.minPrice !== undefined) query.set('minPrice', String(params.minPrice))
  if (params.maxPrice !== undefined) query.set('maxPrice', String(params.maxPrice))
  if (params.sortBy !== undefined) query.set('sortBy', params.sortBy)
  if (params.sortOrder !== undefined) query.set('sortOrder', params.sortOrder)
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.limit !== undefined) query.set('limit', String(params.limit))
  return request<ListingPage>(`/api/listings?${query}`)
}

export function getListings(): Promise<Listing[]> {
  return request<Listing[]>('/listings')
}

export function getListing(id: number): Promise<Listing> {
  return request<Listing>(`/listings/${id}`)
}

export function getCategories(): Promise<Category[]> {
  return request<Category[]>('/listings/categories')
}

export function createListing(input: {
  itemName: string
  description: string
  price: number
  condition: Condition
  category: Category
}): Promise<Listing> {
  return request<Listing>('/listings', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateListing(
  id: number,
  input: {
    itemName: string
    description: string
    price: number
    condition: Condition
    category: Category
  },
): Promise<Listing> {
  return request<Listing>(`/listings/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function deleteListing(id: number): Promise<void> {
  return request<void>(`/listings/${id}`, { method: 'DELETE' })
}
