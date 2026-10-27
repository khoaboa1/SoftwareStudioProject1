import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../services/httpClient'
import { getErrorMessage } from './apiError'
import { useAsyncResource } from './useAsyncResource'

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (error: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

describe('useAsyncResource', () => {
  it('starts loading and becomes ready with the loaded data', async () => {
    const request = deferred<string>()
    const { result } = renderHook(() => useAsyncResource(() => request.promise, []))

    expect(result.current.state).toEqual({ status: 'loading' })
    await act(async () => request.resolve('loaded'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'loaded' })
  })

  it('exposes the raw ApiError on API failure', async () => {
    const request = deferred<string>()
    const error = new ApiError(500, 'Server down')
    const { result } = renderHook(() => useAsyncResource(() => request.promise, []))

    await act(async () => request.reject(error))
    expect(result.current.state.status).toBe('error')
    if (result.current.state.status !== 'error') throw new Error('Expected error state')
    expect(result.current.state.error).toBe(error)
  })

  it('exposes the raw error on network failure', async () => {
    const request = deferred<string>()
    const error = new TypeError('Failed to fetch')
    const { result } = renderHook(() => useAsyncResource(() => request.promise, []))

    await act(async () => request.reject(error))
    expect(result.current.state.status).toBe('error')
    if (result.current.state.status !== 'error') throw new Error('Expected error state')
    expect(result.current.state.error).toBe(error)
    expect(getErrorMessage(result.current.state.error, 'fallback')).toBe('fallback')
  })

  it('ignores a response that arrives after unmount', async () => {
    const request = deferred<string>()
    const onSuccess = vi.fn()
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    try {
      const { unmount } = renderHook(() => useAsyncResource(() => request.promise, [], { onSuccess }))
      unmount()
      await act(async () => request.resolve('late'))
      expect(onSuccess).not.toHaveBeenCalled()
      expect(consoleError).not.toHaveBeenCalled()
    } finally {
      consoleError.mockRestore()
    }
  })

  it('ignores a response from a superseded request', async () => {
    const first = deferred<string>()
    const second = deferred<string>()
    const load = vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const onSuccess = vi.fn()
    const { result, rerender } = renderHook(
      ({ key }) => useAsyncResource(load, [key], { onSuccess }),
      { initialProps: { key: 'first' } },
    )

    rerender({ key: 'second' })
    await act(async () => second.resolve('new'))
    await act(async () => first.resolve('old'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'new' })
    expect(onSuccess).toHaveBeenCalledTimes(1)
    expect(onSuccess).toHaveBeenCalledWith('new')
  })

  it('returns to loading when deps change', async () => {
    const first = deferred<string>()
    const second = deferred<string>()
    const load = vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const { result, rerender } = renderHook(
      ({ key }) => useAsyncResource(load, [key]),
      { initialProps: { key: 'first' } },
    )

    await act(async () => first.resolve('first data'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'first data' })
    rerender({ key: 'second' })
    expect(result.current.state).toEqual({ status: 'loading' })
    await act(async () => second.resolve('second data'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'second data' })
  })

  it('reload re-runs load', async () => {
    const first = deferred<string>()
    const second = deferred<string>()
    const load = vi.fn().mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const { result } = renderHook(() => useAsyncResource(load, []))

    await act(async () => first.resolve('first data'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'first data' })
    act(() => result.current.reload())
    expect(load).toHaveBeenCalledTimes(2)
    expect(result.current.state).toEqual({ status: 'loading' })
    await act(async () => second.resolve('second data'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'second data' })
  })

  it('does not call load while disabled', async () => {
    const load = vi.fn().mockResolvedValue('loaded')
    const { result, rerender } = renderHook(
      ({ enabled }) => useAsyncResource(load, [], { enabled }),
      { initialProps: { enabled: false } },
    )

    expect(result.current.state).toEqual({ status: 'idle' })
    expect(load).not.toHaveBeenCalled()
    rerender({ enabled: true })
    await waitFor(() => expect(result.current.state).toEqual({ status: 'ready', data: 'loaded' }))
    expect(load).toHaveBeenCalledTimes(1)
  })

  it('stays loading when onSuccess reports it handled the result', async () => {
    const request = deferred<string>()
    const onSuccess = vi.fn().mockReturnValue(true)
    const { result } = renderHook(() => useAsyncResource(() => request.promise, [], { onSuccess }))

    await act(async () => request.resolve('loaded'))
    expect(onSuccess).toHaveBeenCalledWith('loaded')
    expect(result.current.state).toEqual({ status: 'loading' })
  })

  it('stays loading when onError reports it handled the error', async () => {
    const request = deferred<string>()
    const error = new ApiError(401, 'Unauthorized')
    const onError = vi.fn().mockReturnValue(true)
    const { result } = renderHook(() => useAsyncResource(() => request.promise, [], { onError }))

    await act(async () => request.reject(error))
    expect(onError).toHaveBeenCalledWith(error)
    expect(result.current.state).toEqual({ status: 'loading' })
  })

  it('calls the latest onError passed in, not the first', async () => {
    const request = deferred<string>()
    const firstOnError = vi.fn()
    const latestOnError = vi.fn()
    const { rerender } = renderHook(
      ({ onError }) => useAsyncResource(() => request.promise, [], { onError }),
      { initialProps: { onError: firstOnError } },
    )

    rerender({ onError: latestOnError })
    const error = new ApiError(500, 'Server down')
    await act(async () => request.reject(error))
    expect(firstOnError).not.toHaveBeenCalled()
    expect(latestOnError).toHaveBeenCalledOnce()
    expect(latestOnError).toHaveBeenCalledWith(error)
  })

  it('setData replaces ready data without reloading', async () => {
    const load = vi.fn().mockResolvedValue('original')
    const { result } = renderHook(() => useAsyncResource(load, []))

    await waitFor(() => expect(result.current.state).toEqual({ status: 'ready', data: 'original' }))
    act(() => result.current.setData('replacement'))
    expect(result.current.state).toEqual({ status: 'ready', data: 'replacement' })
    expect(load).toHaveBeenCalledTimes(1)
  })
})
