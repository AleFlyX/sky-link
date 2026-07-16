import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useUserStore } from '../stores/user'

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
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
})
