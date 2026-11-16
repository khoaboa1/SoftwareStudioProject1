import { afterEach, expect, it, vi } from 'vitest'
import {
  createListing,
  updateListingStatus,
  uploadListingImage,
} from './listingService'

afterEach(() => vi.unstubAllGlobals())

it('uploadListingImage posts multipart to /listings/images and returns url', async () => {
  const fetchMock = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify({ url: 'https://a' })))
  vi.stubGlobal('fetch', fetchMock)
  expect(
    await uploadListingImage(new Blob(['photo'], { type: 'image/jpeg' }))
  ).toEqual({ url: 'https://a' })
  const [path, options] = fetchMock.mock.calls[0]
  expect(path).toMatch(/\/listings\/images$/)
  expect(options.method).toBe('POST')
  expect(options.body).toBeInstanceOf(FormData)
  expect((options.body as FormData).get('file')).toBeInstanceOf(File)
  expect(((options.body as FormData).get('file') as File).name).toBe(
    'photo.jpg'
  )
})

it('createListing sends imageUrls', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  await createListing({
    itemName: 'Lamp',
    description: 'Works',
    price: 10,
    condition: 'GOOD',
    category: 'FURNITURE',
    imageUrls: ['https://a'],
  })
  expect(JSON.parse(fetchMock.mock.calls[0][1].body).imageUrls).toEqual([
    'https://a',
  ])
})

it('updateListingStatus patches the status and returns the listing', async () => {
  const fetchMock = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify({ id: 5, status: 'SOLD' })))
  vi.stubGlobal('fetch', fetchMock)
  expect(await updateListingStatus(5, 'SOLD')).toEqual({
    id: 5,
    status: 'SOLD',
  })
  const [path, options] = fetchMock.mock.calls[0]
  expect(path).toMatch(/\/listings\/5\/status$/)
  expect(options.method).toBe('PATCH')
  expect(JSON.parse(options.body)).toEqual({ status: 'SOLD' })
})
