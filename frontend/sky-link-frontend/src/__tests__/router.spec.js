import { afterEach, describe, expect, it } from 'vitest'

import router from '../router'
import { TOKEN_KEY } from '../utils/request'

const USER_STORAGE_KEY = 'skylink_user'

describe('router auth redirect', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('redirects authenticated users away from the login page when a token exists', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-123')

    await router.push('/register?seed=token')
    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('dashboard')
    expect(router.currentRoute.value.fullPath).toBe('/app/dashboard')
  })

  it('redirects authenticated users away from the login page when only user info exists', async () => {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify({ id: 1001, name: 'SkyLink User' }))

    await router.push('/register?seed=user')
    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('dashboard')
    expect(router.currentRoute.value.fullPath).toBe('/app/dashboard')
  })

  it('registers independent role and permission management routes', () => {
    expect(router.resolve('/app/roles').name).toBe('roles')
    expect(router.resolve('/app/permissions').name).toBe('permissions')
  })
})
