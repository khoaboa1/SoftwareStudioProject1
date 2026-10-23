import { WarningCircle } from '@phosphor-icons/react'
import { Button } from './ui/Button'

export function FeedError({ onRetry }: { onRetry: () => void }) {
  return (
    <div role="alert" className="rounded-2xl border border-dashed border-zinc-300 bg-white p-10 text-center">
      <WarningCircle aria-hidden="true" className="mx-auto mb-4 h-10 w-10 text-red-500" weight="duotone" />
      <h2 className="text-lg font-semibold text-zinc-900">We couldn't load listings</h2>
      <p className="mt-2 text-sm text-zinc-500">Check your connection and try again.</p>
      <Button type="button" fullWidth={false} className="mt-5" onClick={onRetry}>Retry</Button>
    </div>
  )
}
