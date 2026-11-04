import { useState } from 'react'
import { Armchair, CookingPot, Lamp, Laptop, Package, TShirt, type Icon } from '@phosphor-icons/react'
import { CATEGORY_LABELS } from '../lib/listingOptions'
import type { Category } from '../services/listingService'

const CATEGORY_ICONS: Record<Category, Icon> = {
  FURNITURE: Armchair,
  ELECTRONICS: Laptop,
  KITCHEN: CookingPot,
  DECOR: Lamp,
  CLOTHING: TShirt,
  OTHER: Package,
}

export function ListingThumbnail({ category, imageUrl, itemName, className }: {
  category: Category
  imageUrl?: string | null
  itemName?: string
  className?: string
}) {
  const CategoryIcon = CATEGORY_ICONS[category]
  const [failedUrl, setFailedUrl] = useState<string | null>(null)

  if (imageUrl && failedUrl !== imageUrl) {
    return (
      <div className={`flex aspect-[4/3] items-center justify-center bg-accent-50 ${className ?? ''}`}>
        <img
          src={imageUrl}
          alt={itemName ?? `${CATEGORY_LABELS[category]} listing`}
          loading="lazy"
          className="h-full w-full object-cover"
          onError={() => setFailedUrl(imageUrl)}
        />
      </div>
    )
  }

  return (
    <div
      role="img"
      aria-label={`${CATEGORY_LABELS[category]} listing`}
      className={`flex aspect-[4/3] items-center justify-center bg-accent-50 ${className ?? ''}`}
    >
      <CategoryIcon aria-hidden="true" className="h-16 w-16 text-accent-400" weight="duotone" />
    </div>
  )
}
