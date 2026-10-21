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

export function ListingThumbnail({ category, className }: { category: Category; className?: string }) {
  const CategoryIcon = CATEGORY_ICONS[category]

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
