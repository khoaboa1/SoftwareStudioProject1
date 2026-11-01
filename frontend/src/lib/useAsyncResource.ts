import { useEffect, useRef, useState, type DependencyList } from 'react'

export type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ready'; data: T }

export interface AsyncResourceOptions<T> {
  enabled?: boolean
  onSuccess?: (data: T) => boolean | void
  onError?: (error: unknown) => boolean | void
}

export function useAsyncResource<T>(
  load: () => Promise<T>,
  deps: DependencyList,
  options?: AsyncResourceOptions<T>,
): {
  state: AsyncState<T>
  reload: () => void
  setData: (data: T) => void
} {
  const enabled = options?.enabled ?? true
  const [state, setState] = useState<AsyncState<T>>(enabled ? { status: 'loading' } : { status: 'idle' })
  const [reloadToken, setReloadToken] = useState(0)
  const callbacks = useRef({ load, onSuccess: options?.onSuccess, onError: options?.onError })
  callbacks.current = { load, onSuccess: options?.onSuccess, onError: options?.onError }

  useEffect(() => {
    if (!enabled) {
      setState({ status: 'idle' })
      return
    }

    let cancelled = false
    setState({ status: 'loading' })
    callbacks.current.load()
      .then((data) => {
        if (cancelled) return
        if (callbacks.current.onSuccess?.(data) === true) return
        setState({ status: 'ready', data })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (callbacks.current.onError?.(error) === true) return
        setState({ status: 'error', error })
      })

    return () => { cancelled = true }
  // oxlint-disable-next-line react-hooks/exhaustive-deps -- The caller's deps are the request key; callbacks are read from the latest render.
  }, [enabled, reloadToken, ...deps])

  return {
    state,
    reload: () => setReloadToken((token) => token + 1),
    setData: (data) => setState({ status: 'ready', data }),
  }
}
