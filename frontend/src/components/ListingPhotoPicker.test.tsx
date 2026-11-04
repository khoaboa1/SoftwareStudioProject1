import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ListingPhotoPicker, type ListingPhoto } from './ListingPhotoPicker'
import { ListingForm } from './ListingForm'
import { resizeImage } from '../lib/resizeImage'
import { uploadListingImage } from '../services/listingService'

vi.mock('../lib/resizeImage', () => ({ resizeImage: vi.fn() }))
vi.mock('../services/listingService', () => ({ uploadListingImage: vi.fn() }))

function Harness({ initial = [], max = 8 }: { initial?: ListingPhoto[]; max?: number }) {
  const [photos, setPhotos] = useState(initial)
  return <><ListingPhotoPicker photos={photos} onPhotosChange={setPhotos} max={max} />
    <output data-testid="urls">{photos.map(p => p.url ?? p.status).join(',')}</output></>
}

const ready = (id: string): ListingPhoto => ({ id, url: `https://${id}`, previewUrl: `https://${id}`, status: 'ready' })

beforeEach(() => {
  vi.mocked(resizeImage).mockReset().mockResolvedValue(new Blob(['jpg'], { type: 'image/jpeg' }))
  vi.mocked(uploadListingImage).mockReset().mockResolvedValue({ url: 'https://new' })
  vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:preview'), revokeObjectURL: vi.fn() })
})

it('uploads picked photos and shows them in order with a Cover badge on the first', async () => {
  render(<Harness />)
  await userEvent.setup().upload(screen.getByLabelText('Add photos'), [new File(['a'], 'a.png', { type: 'image/png' }), new File(['b'], 'b.png', { type: 'image/png' })])
  await waitFor(() => expect(screen.getByTestId('urls')).toHaveTextContent('https://new,https://new'))
  expect(screen.getAllByText('Cover')).toHaveLength(1)
  expect(uploadListingImage).toHaveBeenCalledTimes(2)
})

it('disables submit while a photo is uploading', async () => {
  vi.mocked(resizeImage).mockReturnValue(new Promise(() => {}))
  render(<ListingForm itemName="Lamp" onItemNameChange={() => {}} description="Works" onDescriptionChange={() => {}} price="10" onPriceChange={() => {}} condition="GOOD" onConditionChange={() => {}} category="FURNITURE" onCategoryChange={() => {}} categoryOptions={['FURNITURE']} status="idle" error={null} submitLabel="Post listing" onSubmit={() => {}} photos={[{ id: '1', url: null, previewUrl: 'blob:one', status: 'uploading' }]} onPhotosChange={() => {}} />)
  expect(screen.getByRole('button', { name: 'Post listing' })).toBeDisabled()
})

it('failed upload shows an error', async () => {
  vi.mocked(uploadListingImage).mockRejectedValue(new Error('Upload failed'))
  render(<Harness />)
  await userEvent.setup().upload(screen.getByLabelText('Add photos'), new File(['a'], 'a.png', { type: 'image/png' }))
  expect(await screen.findByText('Upload failed')).toBeInTheDocument()
  expect(screen.getByTestId('urls')).toHaveTextContent('error')
})

it('caps photos at 8', async () => {
  render(<Harness initial={['a', 'b', 'c', 'd', 'e'].map(ready)} />)
  await userEvent.setup().upload(screen.getByLabelText('Add photos'), Array.from({ length: 6 }, (_, i) => new File(['a'], `${i}.png`, { type: 'image/png' })))
  await waitFor(() => expect(screen.getAllByRole('button', { name: /Remove photo/ })).toHaveLength(8))
  expect(screen.getByText('You can add up to 8 photos.')).toBeInTheDocument()
})

it('removes a photo', async () => {
  render(<Harness initial={[ready('a'), ready('b')]} />)
  await userEvent.setup().click(screen.getByRole('button', { name: 'Remove photo 1' }))
  expect(screen.getByTestId('urls')).toHaveTextContent('https://b')
})

it('moves a photo left and right', async () => {
  render(<Harness initial={[ready('a'), ready('b'), ready('c')]} />)
  await userEvent.setup().click(screen.getByRole('button', { name: 'Move photo 2 left' }))
  expect(screen.getByTestId('urls')).toHaveTextContent('https://b,https://a,https://c')
  await userEvent.setup().click(screen.getByRole('button', { name: 'Move photo 1 right' }))
  expect(screen.getByTestId('urls')).toHaveTextContent('https://a,https://b,https://c')
  expect(screen.getAllByText('Cover')).toHaveLength(1)
})
