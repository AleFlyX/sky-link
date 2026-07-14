import { beforeEach, describe, expect, it, vi } from 'vitest'

import { cookieService, request, service } from '../utils/request'

function respondWith(data) {
  return service.request({
    url: '/test',
    adapter: async (config) => ({
      config,
      data,
      headers: {},
      status: 200,
      statusText: 'OK',
    }),
  })
}

describe('response business code handling', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null),
      removeItem: vi.fn(),
      setItem: vi.fn(),
    })
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
