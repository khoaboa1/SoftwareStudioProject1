import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createAuthFacade } from './authFacade'
import { ApiError, type LoginResult } from './authService'

vi.mock('../lib/device', () => ({
  getOrCreateDeviceId: () => 'existing-browser',
}))

const student = {
  id: 1,
  studentName: 'Jane Doe',
  email: 'jane@tulane.edu',
  emailVerified: true,
  sellingItems: null,
  dormLocation: null,
}

describe('authentication facade and login adapter', () => {
  const fetchMock = vi.fn()
  const setStudent = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    setStudent.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function respond(body: unknown, status = 200) {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(body), { status }))
  }

  function expectRequest(path: string, body?: unknown) {
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toMatch(new RegExp(`${path}$`))
    expect(options.method).toBe('POST')
    expect(options.credentials).toBe('include')
    expect(options.headers['Content-Type']).toBe('application/json')
    if (body !== undefined) expect(JSON.parse(options.body)).toEqual(body)
  }

  it('preserves the login API and adapts its response before updating auth state', async () => {
    const response = {
      requiresPin: false, message: 'Login successful',
      id: 1, studentName: 'Jane Doe', email: 'jane@tulane.edu',
    }
    respond(response)

    const result = await createAuthFacade(setStudent).signIn({
      email: student.email, password: 'Password123!',
    })

    expectRequest('/auth/login', {
      email: student.email, password: 'Password123!', deviceId: 'existing-browser',
    })
    expect(result).toEqual(response)
    expect(setStudent).toHaveBeenCalledExactlyOnceWith(student)
  })

  it('preserves explicit student values instead of replacing them with defaults', async () => {
    const response = {
      ...student, requiresPin: false, message: 'Login successful',
      emailVerified: false, sellingItems: ['Lamp'], dormLocation: 'Wall',
    }
    respond(response)

    await createAuthFacade(setStudent).signIn({ email: student.email, password: 'Password123!' })

    expect(setStudent).toHaveBeenCalledExactlyOnceWith({
      ...student, emailVerified: false, sellingItems: ['Lamp'], dormLocation: 'Wall',
    })
  })

  it('does not authenticate a PIN-required response even if it includes identity fields', async () => {
    const response = { ...student, requiresPin: true, message: 'Check your email' }
    respond(response)

    const result = await createAuthFacade(setStudent).signIn({
      email: student.email, password: 'Password123!',
    })

    expect(result).toEqual(response)
    expect(setStudent).not.toHaveBeenCalled()
  })

  it.each(['id', 'studentName', 'email'] as const)(
    'preserves the existing no-auth-update behavior when %s is missing', async (field) => {
      const response: LoginResult = { ...student, requiresPin: false, message: 'Login successful' }
      delete response[field]
      respond(response)

      expect(await createAuthFacade(setStudent).signIn({
        email: student.email, password: 'Password123!',
      })).toEqual(response)
      expect(setStudent).not.toHaveBeenCalled()
    },
  )

  it('preserves the PIN API and publishes the returned student', async () => {
    respond(student)

    const result = await createAuthFacade(setStudent).verifySignIn({
      email: student.email, pin: '123456',
    })

    expectRequest('/auth/verify-pin', {
      email: student.email, pin: '123456', deviceId: 'existing-browser',
    })
    expect(result).toEqual(student)
    expect(setStudent).toHaveBeenCalledExactlyOnceWith(student)
  })

  it('preserves the logout API and clears auth only after the server succeeds', async () => {
    let finishRequest!: (response: Response) => void
    fetchMock.mockReturnValueOnce(new Promise<Response>((resolve) => { finishRequest = resolve }))

    const result = createAuthFacade(setStudent).signOut()

    expectRequest('/auth/logout')
    expect(setStudent).not.toHaveBeenCalled()
    finishRequest(new Response(null, { status: 204 }))
    await expect(result).resolves.toBeUndefined()
    expect(setStudent).toHaveBeenCalledExactlyOnceWith(null)
  })

  it.each(['signIn', 'verifySignIn', 'signOut'] as const)(
    'propagates %s API failures without publishing an auth change', async (action) => {
      respond({ message: 'Request rejected' }, 401)
      const auth = createAuthFacade(setStudent)
      const pending = action === 'signIn'
        ? auth.signIn({ email: student.email, password: 'Password123!' })
        : action === 'verifySignIn'
          ? auth.verifySignIn({ email: student.email, pin: '123456' })
          : auth.signOut()

      await expect(pending).rejects.toBeInstanceOf(ApiError)
      await expect(pending).rejects.toMatchObject({ status: 401, message: 'Request rejected' })
      expect(setStudent).not.toHaveBeenCalled()
    },
  )

  it('propagates network failures without publishing an auth change', async () => {
    const error = new TypeError('Network unavailable')
    fetchMock.mockRejectedValueOnce(error)

    await expect(createAuthFacade(setStudent).signIn({
      email: student.email, password: 'Password123!',
    })).rejects.toBe(error)
    expect(setStudent).not.toHaveBeenCalled()
  })

  it('keeps auth observers separate for different facade clients', async () => {
    const otherObserver = vi.fn()
    const first = createAuthFacade(setStudent)
    createAuthFacade(otherObserver)
    respond(student)

    await first.verifySignIn({ email: student.email, pin: '123456' })

    expect(setStudent).toHaveBeenCalledExactlyOnceWith(student)
    expect(otherObserver).not.toHaveBeenCalled()
  })
})
