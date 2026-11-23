import { SealCheck } from '@phosphor-icons/react'

export function VerifiedBadge() {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-accent-50 px-2 py-0.5 text-xs font-medium text-accent-800">
      <SealCheck aria-hidden="true" weight="fill" size={12} />
      Verified Student
    </span>
  )
}
