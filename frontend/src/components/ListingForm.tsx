import type { FormEvent } from 'react'
import { Button } from './ui/Button'
import { FormAlert } from './ui/FormAlert'
import { SelectField } from './ui/SelectField'
import { TextField } from './ui/TextField'
import { CATEGORY_LABELS, CONDITION_OPTIONS } from '../lib/listingOptions'
import type { Category, Condition } from '../services/listingService'

type ListingFormProps = {
  itemName: string
  onItemNameChange: (value: string) => void
  description: string
  onDescriptionChange: (value: string) => void
  price: string
  onPriceChange: (value: string) => void
  condition: Condition
  onConditionChange: (value: Condition) => void
  category: Category
  onCategoryChange: (value: Category) => void
  categoryOptions: Category[]
  status: 'idle' | 'submitting' | 'error'
  error: string | null
  submitLabel: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function ListingForm({
  itemName,
  onItemNameChange,
  description,
  onDescriptionChange,
  price,
  onPriceChange,
  condition,
  onConditionChange,
  category,
  onCategoryChange,
  categoryOptions,
  status,
  error,
  submitLabel,
  onSubmit,
}: ListingFormProps) {
  return (
    <form
      onSubmit={onSubmit}
      className="mb-8 flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm"
    >
      {status === 'error' && error && <FormAlert kind="error">{error}</FormAlert>}

      <TextField
        label="Item name"
        placeholder="Desk Lamp"
        value={itemName}
        onChange={(e) => onItemNameChange(e.target.value)}
      />

      <div className="flex flex-col gap-1.5">
        <label htmlFor="listing-description" className="text-sm font-medium text-zinc-800">
          Description
        </label>
        <textarea
          id="listing-description"
          rows={3}
          placeholder="Barely used, works great"
          value={description}
          onChange={(e) => onDescriptionChange(e.target.value)}
          className="w-full rounded-lg border border-zinc-300 px-3.5 py-2.5 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
        />
      </div>

      <TextField
        label="Price (USD)"
        type="number"
        min="0"
        step="0.01"
        placeholder="10.00"
        value={price}
        onChange={(e) => onPriceChange(e.target.value)}
      />

      <SelectField
        label="Condition"
        options={CONDITION_OPTIONS}
        value={condition}
        onChange={(e) => onConditionChange(e.target.value as Condition)}
      />

      <SelectField
        label="Category"
        options={categoryOptions.map((value) => ({ value, label: CATEGORY_LABELS[value] }))}
        value={category}
        onChange={(e) => onCategoryChange(e.target.value as Category)}
      />

      <Button type="submit" loading={status === 'submitting'}>
        {submitLabel}
      </Button>
    </form>
  )
}
