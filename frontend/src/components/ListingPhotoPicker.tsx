import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, Plus, SpinnerGap, X } from '@phosphor-icons/react'
import { resizeImage } from '../lib/resizeImage'
import { uploadListingImage } from '../services/listingService'
import { Button } from './ui/Button'

export type ListingPhoto = {
  id: string
  url: string | null
  previewUrl: string
  status: 'uploading' | 'ready' | 'error'
  error?: string
}

type Props = {
  photos: ListingPhoto[]
  onPhotosChange: (update: (prev: ListingPhoto[]) => ListingPhoto[]) => void
  max?: number
}

export function ListingPhotoPicker({ photos, onPhotosChange, max = 8 }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const objectUrls = useRef(new Set<string>())
  const [limitError, setLimitError] = useState(false)

  useEffect(() => () => {
    for (const url of objectUrls.current) URL.revokeObjectURL(url)
    objectUrls.current.clear()
  }, [])

  function remove(index: number) {
    const url = photos[index].previewUrl
    if (objectUrls.current.delete(url)) URL.revokeObjectURL(url)
    onPhotosChange((prev) => prev.filter((_, i) => i !== index))
  }

  function move(index: number, offset: number) {
    onPhotosChange((prev) => {
      const next = [...prev]
      const target = index + offset
      if (target < 0 || target >= next.length) return prev
      ;[next[index], next[target]] = [next[target], next[index]]
      return next
    })
  }

  function pick(files: FileList | null) {
    if (!files) return
    const selected = Array.from(files)
    const accepted = selected.slice(0, Math.max(0, max - photos.length))
    setLimitError(selected.length > accepted.length)
    for (const file of accepted) {
      const previewUrl = URL.createObjectURL(file)
      objectUrls.current.add(previewUrl)
      const id = crypto.randomUUID()
      onPhotosChange((prev) => [...prev, { id, url: null, previewUrl, status: 'uploading' }])
      resizeImage(file).then(uploadListingImage).then(({ url }) => {
        onPhotosChange((prev) => prev.map((photo) => photo.id === id ? { ...photo, url, status: 'ready' } : photo))
      }).catch((error: unknown) => {
        onPhotosChange((prev) => prev.map((photo) => photo.id === id
          ? { ...photo, status: 'error', error: error instanceof Error ? error.message : 'Could not upload photo.' }
          : photo))
      })
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <div>
        <h2 className="text-sm font-medium text-zinc-800">Photos</h2>
        <p className="text-sm text-zinc-500">Up to 8 photos. The first one is the cover.</p>
      </div>
      <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" multiple
        aria-label="Add photos" className="sr-only" onChange={(event) => {
          pick(event.target.files)
          event.target.value = ''
        }} />
      {photos.length < max && <Button type="button" fullWidth={false} onClick={() => inputRef.current?.click()}>
        <Plus aria-hidden="true" className="mr-2" />Add photos
      </Button>}
      {limitError && <p role="alert" className="text-sm text-red-700">You can add up to 8 photos.</p>}
      {photos.length > 0 && <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {photos.map((photo, index) => <div key={photo.id} className="relative overflow-hidden rounded-lg border border-zinc-200 bg-zinc-100">
          <div className="relative aspect-[4/3]">
            <img src={photo.previewUrl} alt={`Photo ${index + 1}`} className="h-full w-full object-cover" />
            {index === 0 && <span className="absolute left-2 top-2 rounded bg-accent-700 px-2 py-0.5 text-xs font-medium text-white">Cover</span>}
            {photo.status === 'uploading' && <div className="absolute inset-0 flex items-center justify-center bg-zinc-900/50" role="status" aria-label={`Uploading photo ${index + 1}`}>
              <SpinnerGap aria-hidden="true" className="h-7 w-7 animate-spin text-white" />
            </div>}
          </div>
          {photo.status === 'error' && <p className="px-2 py-1 text-xs text-red-700">{photo.error}</p>}
          <div className="flex justify-end gap-1 p-1">
            {photo.status !== 'error' && <>
              <button type="button" aria-label={`Move photo ${index + 1} left`} disabled={index === 0} onClick={() => move(index, -1)} className="rounded p-1 text-zinc-700 hover:bg-zinc-200 disabled:opacity-40"><ArrowLeft aria-hidden="true" /></button>
              <button type="button" aria-label={`Move photo ${index + 1} right`} disabled={index === photos.length - 1} onClick={() => move(index, 1)} className="rounded p-1 text-zinc-700 hover:bg-zinc-200 disabled:opacity-40"><ArrowRight aria-hidden="true" /></button>
            </>}
            <button type="button" aria-label={`Remove photo ${index + 1}`} onClick={() => remove(index)} className="rounded p-1 text-zinc-700 hover:bg-zinc-200"><X aria-hidden="true" /></button>
          </div>
        </div>)}
      </div>}
    </section>
  )
}
