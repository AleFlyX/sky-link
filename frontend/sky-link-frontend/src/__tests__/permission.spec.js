import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { hasPermission, permission } from '../directives/permission'
import { useUserStore } from '../stores/user'

const PermissionProbe = defineComponent({
  template: `
    <div>
      <button v-permission="'task:create'">创建任务</button>
      <button v-permission="{ permissions: ['task:create', 'task:delete'], mode: 'all' }">
        管理任务
      </button>
    </div>
  `,
})

describe('v-permission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('supports any and all permission checks', () => {
    expect(hasPermission(['task:list'], 'task:list')).toBe(true)
    expect(hasPermission(['task:list'], ['task:list', 'task:create'])).toBe(true)
    expect(
      hasPermission(['task:list'], { permissions: ['task:list', 'task:create'], mode: 'all' }),
    ).toBe(false)
  })

  it('hides unauthorized elements and reacts to permission changes', async () => {
    const userStore = useUserStore()
    userStore.setUser({ permissions: ['task:list'] })
    const wrapper = mount(PermissionProbe, {
      global: {
        directives: { permission },
      },
    })

    const buttons = wrapper.findAll('button')
    expect(buttons[0].element.style.display).toBe('none')
    expect(buttons[1].element.style.display).toBe('none')

    userStore.patchUser({ permissions: ['task:create', 'task:delete'] })
    await nextTick()

    expect(buttons[0].element.style.display).toBe('')
    expect(buttons[1].element.style.display).toBe('')
  })
})
