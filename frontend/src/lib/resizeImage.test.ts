import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { resizeImage } from './resizeImage'

const context = { drawImage: vi.fn() }
const toBlob = vi.fn((callback: BlobCallback) => callback(new Blob(['jpg'], { type: 'image/jpeg' })))

beforeEach(() => {
  vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue({ width: 4000, height: 3000, close: vi.fn() }))
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context as unknown as CanvasRenderingContext2D)
  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(toBlob)
})
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); toBlob.mockClear(); context.drawImage.mockClear() })

it('scales a 4000x3000 image to 1600x1200', async () => {
  await resizeImage(new File(['photo'], 'photo.png'))
  expect(context.drawImage).toHaveBeenCalledWith(expect.anything(), 0, 0, 1600, 1200)
})

it('does not upscale a 800x600 image', async () => {
  vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue({ width: 800, height: 600, close: vi.fn() }))
  await resizeImage(new File(['photo'], 'photo.png'))
  expect(context.drawImage).toHaveBeenCalledWith(expect.anything(), 0, 0, 800, 600)
})

it('outputs image/jpeg at 0.85', async () => {
  const result = await resizeImage(new File(['photo'], 'photo.png'))
  expect(result.type).toBe('image/jpeg')
  expect(toBlob).toHaveBeenCalledWith(expect.any(Function), 'image/jpeg', 0.85)
})

it('throws the unsupported-format message when decoding fails', async () => {
  vi.stubGlobal('createImageBitmap', vi.fn().mockRejectedValue(new Error('decode')))
  await expect(resizeImage(new File(['bad'], 'bad.txt')))
    .rejects.toThrow("This photo format isn't supported. Use JPEG, PNG or WebP.")
})
