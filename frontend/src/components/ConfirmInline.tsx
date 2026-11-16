import { useState } from 'react'
import { Button } from './ui/Button'
import { FormAlert } from './ui/FormAlert'
import { getErrorMessage } from '../lib/apiError'

export function ConfirmInline({
  message,
  confirmLabel,
  onConfirm,
  onCancel,
}: {
  message: string
  confirmLabel: string
  onConfirm: () => Promise<void>
  onCancel: () => void
}) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    if (loading) return
    setLoading(true)
    setError(null)
    try {
      await onConfirm()
    } catch (cause) {
      setError(getErrorMessage(cause, 'Could not complete this action.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mt-4 border-l-2 border-accent-700 pl-4">
      <p className="mb-3 text-sm text-zinc-800">{message}</p>
      {error && <FormAlert kind="error">{error}</FormAlert>}
      <div className="flex flex-wrap gap-3">
        <Button
          type="button"
          fullWidth={false}
          loading={loading}
          onClick={confirm}
        >
          {confirmLabel}
        </Button>
        <button
          type="button"
          disabled={loading}
          onClick={onCancel}
          className="text-sm font-medium text-zinc-600 hover:text-zinc-900"
        >
          Cancel
        </button>
      </div>
    </div>
  )
}
