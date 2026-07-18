import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { getCurrentUser } from '../api/user'
import router from '../router'
import { TOKEN_KEY } from '../utils/request'

const USER_STORAGE_KEY = 'skylink_user'

vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn(),
}))

describe('router auth redirect', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(getCurrentUser).mockResolvedValue({
      data: {
        userId: 1001,
        username: 'chenyt',
        nickname: '陈雨桐',
        departmentName: '产品研发中心',
        permissions: [],
      },
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
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

  it('loads the full current user before entering app pages with a token', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-123')

    await router.push('/app/dashboard?seed=current-user')

    expect(getCurrentUser).toHaveBeenCalledTimes(1)
    expect(JSON.parse(localStorage.getItem(USER_STORAGE_KEY))).toMatchObject({
      department: '产品研发中心',
    })
  })

  it('registers independent role and permission management routes', () => {
    expect(router.resolve('/app/roles').name).toBe('roles')
    expect(router.resolve('/app/permissions').name).toBe('permissions')
  })
})
