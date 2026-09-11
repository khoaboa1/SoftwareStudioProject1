import { useId, type InputHTMLAttributes } from 'react'

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
  helperText?: string
}

export function TextField({
  label,
  error,
  helperText,
  id,
  className,
  ...inputProps
}: TextFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  const errorId = `${fieldId}-error`
  const helperId = `${fieldId}-helper`

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={fieldId} className="text-sm font-medium text-zinc-800">
        {label}
      </label>
      <input
        id={fieldId}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : helperText ? helperId : undefined}
        className={`w-full rounded-lg border px-3.5 py-2.5 text-[15px] text-zinc-900 placeholder:text-zinc-400 outline-none transition-colors ${
          error
            ? 'border-red-400 focus:border-red-500 focus:ring-2 focus:ring-red-100'
            : 'border-zinc-300 focus:border-accent-500 focus:ring-2 focus:ring-accent-100'
        } ${className ?? ''}`}
        {...inputProps}
      />
      {error ? (
        <p id={errorId} className="text-sm text-red-600">
          {error}
        </p>
      ) : helperText ? (
        <p id={helperId} className="text-sm text-zinc-500">
          {helperText}
        </p>
      ) : null}
    </div>
  )
}
