import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { cookieService, request, service, unauthorizedRedirect } from '../utils/request'

function respondWith(data, { url = '/test', status = 200 } = {}) {
  return service.request({
    url,
    adapter: async (config) => ({
      config,
      data,
      headers: {},
      status,
      statusText: status === 200 ? 'OK' : 'Error',
    }),
  })
}

function rejectWithAxiosError(data, { url = '/test', status = 401 } = {}) {
  return service.request({
    url,
    adapter: async (config) =>
      Promise.reject({
        isAxiosError: true,
        config,
        message: 'Request failed with status code 401',
        response: {
          config,
          data,
          headers: {},
          status,
          statusText: 'Unauthorized',
        },
      }),
  })
}

describe('response business code handling', () => {
  beforeEach(() => {
    const storage = new Map()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => storage.get(key) ?? null),
      removeItem: vi.fn((key) => {
        storage.delete(key)
      }),
      setItem: vi.fn((key, value) => {
        storage.set(key, String(value))
      }),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('resolves responses whose business code is 200', async () => {
    const payload = { code: 200, message: 'success', data: { id: 1 } }

    await expect(respondWith(payload)).resolves.toEqual(payload)
  })

  it.each([0, 201, 400, 500])('rejects business code %s', async (code) => {
    await expect(respondWith({ code, message: `error-${code}` })).rejects.toThrow(`error-${code}`)
  })

  it('rejects a string code of 200', async () => {
    await expect(respondWith({ code: '200', message: 'invalid code type' })).rejects.toThrow(
      'invalid code type',
    )
  })

  it.each([{}, [], null, 'success'])('rejects a response without code 200', async (payload) => {
    await expect(respondWith(payload)).rejects.toThrow('Invalid response')
  })

  it('allows a raw download only when the HTTP status is 200', async () => {
    const download = service.request({
      url: '/download',
      rawResponse: true,
      responseType: 'blob',
      adapter: async (config) => ({
        config,
        data: 'binary-content',
        headers: {},
        status: 200,
        statusText: 'OK',
      }),
    })

    await expect(download).resolves.toBe('binary-content')
  })

  it('keeps auth endpoint 401 responses on the login flow', async () => {
    const redirectSpy = vi
      .spyOn(unauthorizedRedirect, 'toUnauthorized')
      .mockImplementation(() => {})

    await expect(
      rejectWithAxiosError(
        {
          message: 'Invalid credentials',
        },
        {
          url: '/auth/login',
        },
      ),
    ).rejects.toThrow('Invalid credentials')

    expect(redirectSpy).not.toHaveBeenCalled()
  })

  it('still redirects protected 401 responses to the unauthorized page', async () => {
    const redirectSpy = vi
      .spyOn(unauthorizedRedirect, 'toUnauthorized')
      .mockImplementation(() => {})

    await expect(
      rejectWithAxiosError(
        {
          message: 'Session expired',
        },
        {
          url: '/app/dashboard',
        },
      ),
    ).rejects.toThrow('Session expired')

    expect(redirectSpy).toHaveBeenCalledTimes(1)
  })

  it('refreshes an expired access token once and retries the original request', async () => {
    localStorage.setItem('skylink_token', 'expired-token')
    const refreshSpy = vi.spyOn(cookieService, 'post').mockResolvedValue({
      accessToken: 'new-token',
      expiresIn: 3600,
    })
    const authHeaders = []

    await expect(
      service.request({
        url: '/protected',
        adapter: async (config) => {
          authHeaders.push(config.headers?.Authorization || null)

          if (authHeaders.length === 1) {
            return Promise.reject({
              isAxiosError: true,
              config,
              message: 'Request failed with status code 401',
              response: {
                config,
                data: { message: 'Session expired' },
                headers: {},
                status: 401,
                statusText: 'Unauthorized',
              },
            })
          }

          return {
            config,
            data: { code: 200, message: 'success', data: { ok: true } },
            headers: {},
            status: 200,
            statusText: 'OK',
          }
        },
      }),
    ).resolves.toEqual({ code: 200, message: 'success', data: { ok: true } })

    expect(refreshSpy).toHaveBeenCalledTimes(1)
    expect(authHeaders).toEqual(['Bearer expired-token', 'Bearer new-token'])
    expect(localStorage.getItem('skylink_token')).toBe('new-token')
  })

  it('rejects a raw download whose HTTP status is not 200', async () => {
    const download = service.request({
      url: '/download',
      rawResponse: true,
      responseType: 'blob',
      adapter: async (config) => ({
        config,
        data: 'binary-content',
        headers: {},
        status: 201,
        statusText: 'Created',
      }),
    })

    await expect(download).rejects.toThrow('Unexpected response status: 201')
  })

  it('provides get, post, put and delete helpers', async () => {
    const methods = []
    const adapter = async (config) => {
      methods.push(config.method)
      return {
        config,
        data: { code: 200, message: 'success', data: null },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    }

    await request.get('/items', { page: 1 }, { adapter })
    await request.post('/items', { name: 'new' }, { adapter })
    await request.put('/items/1', { name: 'updated' }, { adapter })
    await request.delete('/items/1', { adapter })

    expect(methods).toEqual(['get', 'post', 'put', 'delete'])
  })

  it('only enables credentials on the cookie-specific axios instance', () => {
    expect(service.defaults.withCredentials).not.toBe(true)
    expect(cookieService.defaults.withCredentials).toBe(true)
  })
})
