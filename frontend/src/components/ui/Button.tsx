import type { ButtonHTMLAttributes } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  loading?: boolean
  fullWidth?: boolean
}

/** bg-accent-700 (not 500/600) keeps white label text at WCAG AA contrast (~5.7:1). */
export function Button({
  loading,
  disabled,
  children,
  className,
  fullWidth = true,
  ...buttonProps
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      aria-busy={loading}
      className={`inline-flex items-center justify-center rounded-lg bg-accent-700 px-4 py-2.5 text-[15px] font-medium text-white transition-all hover:bg-accent-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 ${fullWidth ? 'w-full' : ''} ${className ?? ''}`}
      {...buttonProps}
    >
      {loading ? 'Please wait…' : children}
    </button>
  )
}
