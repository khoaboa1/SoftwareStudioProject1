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

export function getListings(): Promise<Listing[]> {
  return request<Listing[]>('/listings')
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
