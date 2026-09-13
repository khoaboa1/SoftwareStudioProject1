import { useId, type SelectHTMLAttributes } from 'react'

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string
  options: { value: string; label: string }[]
}

export function SelectField({ label, options, id, className, ...selectProps }: SelectFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={fieldId} className="text-sm font-medium text-zinc-800">
        {label}
      </label>
      <select
        id={fieldId}
        className={`w-full rounded-lg border border-zinc-300 bg-white px-3.5 py-2.5 text-[15px] text-zinc-900 outline-none transition-colors focus:border-accent-500 focus:ring-2 focus:ring-accent-100 ${className ?? ''}`}
        {...selectProps}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  )
}
