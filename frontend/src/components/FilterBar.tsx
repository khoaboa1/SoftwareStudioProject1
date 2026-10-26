import { useEffect, useId, useRef, useState } from 'react'
import { CATEGORY_OPTIONS } from '../lib/listingOptions'
import { isBrowseActive, SORT_OPTIONS, type BrowseFilters } from '../lib/browseParams'
import { SelectField } from './ui/SelectField'
import { TextField } from './ui/TextField'

export type BrowseParamPatch = Partial<Record<'category' | 'minPrice' | 'maxPrice' | 'sort', string | null>>

type FilterBarProps = {
  filters: BrowseFilters
  onChange: (patch: BrowseParamPatch) => void
  onClear: () => void
  debounceMs?: number
}

export function FilterBar({ filters, onChange, onClear, debounceMs = 400 }: FilterBarProps) {
  const [minText, setMinText] = useState(filters.minPrice === null ? '' : String(filters.minPrice))
  const [maxText, setMaxText] = useState(filters.maxPrice === null ? '' : String(filters.maxPrice))
  const [priceError, setPriceError] = useState('')
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const priceErrorId = useId()
  // The error is about the pair, so both boxes turn red. The message is overlaid below them so the row doesn't shift.
  const priceErrorProps = priceError
    ? { 'aria-invalid': true, 'aria-describedby': priceErrorId, className: 'border-red-400! focus:border-red-500! focus:ring-red-100!' }
    : {}

  // oxlint-disable react-hooks/exhaustive-deps -- Local typing should only resync when the URL filters change.
  useEffect(() => {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = null
    // oxlint-disable-next-line react/set-state-in-effect -- Synchronizes an external URL change.
    setMinText(filters.minPrice === null ? '' : String(filters.minPrice))
    // oxlint-disable-next-line react/set-state-in-effect -- Synchronizes an external URL change.
    setMaxText(filters.maxPrice === null ? '' : String(filters.maxPrice))
    // oxlint-disable-next-line react/set-state-in-effect -- Clears validation after an external URL change.
    setPriceError('')
  }, [filters.minPrice, filters.maxPrice])
  // oxlint-enable react-hooks/exhaustive-deps

  useEffect(() => () => {
    if (timer.current !== null) clearTimeout(timer.current)
  }, [])

  function schedulePrices(min: string, max: string) {
    if (timer.current !== null) clearTimeout(timer.current)
    timer.current = setTimeout(() => {
      timer.current = null
      const minPrice = min === '' ? null : Number(min)
      const maxPrice = max === '' ? null : Number(max)
      if (minPrice !== null && maxPrice !== null && minPrice > maxPrice) {
        setPriceError("Min price can't be more than max price")
        return
      }
      setPriceError('')
      onChange({
        minPrice: minPrice === null ? null : String(minPrice),
        maxPrice: maxPrice === null ? null : String(maxPrice),
      })
    }, debounceMs)
  }

  return (
    // Phones: two-column grid (Category + Sort, then Min + Max, then Clear). Wider screens: one row, Sort pushed right.
    <div className="mb-6 grid grid-cols-2 items-start gap-3 sm:flex sm:flex-wrap">
      <div className="order-1 sm:order-none">
        <SelectField
          label="Category"
          value={filters.category?.toLowerCase() ?? ''}
          options={[{ value: '', label: 'All categories' }, ...CATEGORY_OPTIONS.map(({ value, label }) => ({ value: value.toLowerCase(), label }))]}
          onChange={(event) => onChange({ category: event.currentTarget.value || null })}
        />
      </div>
      {/* Condition filter goes here once the backend supports it — see docs/backend-work-ssp1-86.md */}
      <div className="relative order-3 sm:order-none sm:w-36">
        <TextField
          label="Min price"
          type="number"
          min={0}
          step="0.01"
          inputMode="decimal"
          value={minText}
          {...priceErrorProps}
          onChange={(event) => {
            const next = event.currentTarget.value
            setMinText(next)
            schedulePrices(next, maxText)
          }}
        />
        <p id={priceErrorId} aria-live="polite" className="absolute left-0 top-full mt-1 whitespace-nowrap text-xs text-red-600">
          {priceError}
        </p>
      </div>
      <div className="order-4 sm:order-none sm:w-36">
        <TextField
          label="Max price"
          type="number"
          min={0}
          step="0.01"
          inputMode="decimal"
          value={maxText}
          {...priceErrorProps}
          onChange={(event) => {
            const next = event.currentTarget.value
            setMaxText(next)
            schedulePrices(minText, next)
          }}
        />
      </div>
      {isBrowseActive(filters) && (
        <button
          type="button"
          onClick={onClear}
          className="order-5 col-span-2 mt-3 justify-self-start text-sm font-medium text-accent-600 hover:text-accent-700 sm:order-none sm:mt-[26px] sm:py-3"
        >
          Clear filters
        </button>
      )}
      <div className="order-2 sm:order-none sm:ml-auto">
        <SelectField
          label="Sort by"
          options={SORT_OPTIONS}
          value={filters.sort}
          onChange={(event) => onChange({ sort: event.currentTarget.value === 'newest' ? null : event.currentTarget.value })}
        />
      </div>
    </div>
  )
}
