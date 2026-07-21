import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { getCurrentUser } from '../api/user'
import { useUserStore } from '../stores/user'

vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn(),
}))

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    vi.mocked(getCurrentUser).mockReset()
  })

  it('normalizes role labels to Chinese', () => {
    const userStore = useUserStore()

    userStore.setUser({
      name: '陈雨桐',
      roleLabel: 'Administrator',
      roles: [{ roleCode: 'ROLE_SUPER_ADMIN' }],
    })

    expect(userStore.user.roleLabel).toBe('管理员')
  })

  it('normalizes role codes and joins multiple roles with Chinese punctuation', () => {
    const userStore = useUserStore()

    userStore.setUser({
      name: '陈雨桐',
      roles: [
        { roleCode: 'ROLE_ADMIN' },
        { roleName: 'Project Leader' },
        { roleName: '产品管理员' },
      ],
    })

    expect(userStore.user.roleLabel).toBe('管理员、项目主管、产品管理员')
  })

  it('refreshes the full current user profile after login cached partial user info', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      data: {
        userId: 1001,
        username: 'chenyt',
        nickname: '陈雨桐',
        departmentId: 201,
        departmentName: '产品研发中心',
        permissions: ['user:me:get'],
      },
    })

    const userStore = useUserStore()
    userStore.setUser({
      id: 1001,
      account: 'chenyt',
      name: '陈雨桐',
      department: '',
      permissions: [],
    })

    await userStore.ensureCurrentUserLoaded()

    expect(getCurrentUser).toHaveBeenCalledTimes(1)
    expect(userStore.user).toMatchObject({
      id: 1001,
      account: 'chenyt',
      name: '陈雨桐',
      departmentId: 201,
      department: '产品研发中心',
      permissions: ['user:me:get'],
    })
  })
})
