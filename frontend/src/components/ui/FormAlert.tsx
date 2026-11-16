import { CheckCircle, WarningCircle } from '@phosphor-icons/react'

type FormAlertProps = {
  kind: 'success' | 'error'
  children: React.ReactNode
  /** Optional follow-up, such as retrying a request that could not reach the server. */
  action?: { label: string; onClick: () => void }
}

export function FormAlert({ kind, children, action }: FormAlertProps) {
  const isSuccess = kind === 'success'
  return (
    <div
      role="status"
      className={`mb-5 flex items-start gap-2 rounded-lg border px-3.5 py-2.5 text-sm ${
        isSuccess
          ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
          : 'border-red-200 bg-red-50 text-red-700'
      }`}
    >
      {isSuccess ? (
        <CheckCircle weight="fill" className="mt-0.5 h-4 w-4 shrink-0" />
      ) : (
        <WarningCircle weight="fill" className="mt-0.5 h-4 w-4 shrink-0" />
      )}
      <span className="flex-1">{children}</span>
      {action && (
        <button
          type="button"
          onClick={action.onClick}
          className="shrink-0 font-medium underline underline-offset-2 hover:no-underline"
        >
          {action.label}
        </button>
      )}
    </div>
  )
}
