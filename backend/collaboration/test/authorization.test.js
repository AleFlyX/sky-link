import { afterEach, describe, expect, it, vi } from 'vitest'
import { reauthorize } from '../src/authorization.js'

afterEach(() => vi.unstubAllGlobals())
describe('Spring authorization boundary', () => {
  it('unwraps the standard ApiResponse envelope', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => ({ code: 200, data: { allowed: true, permission: 'edit' } }) })))
    const result = await reauthorize({ internalBaseUrl: 'http://spring', serviceToken: 'service' }, { userId: 7, documentId: 42 })
    expect(result).toEqual({ allowed: true, permission: 'edit' })
  })
})
