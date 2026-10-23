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

export function searchListings(params: { q?: string; limit?: number }): Promise<ListingPage> {
  const query = new URLSearchParams()
  if (params.q !== undefined) query.set('q', params.q)
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
