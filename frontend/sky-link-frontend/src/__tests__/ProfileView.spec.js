import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

import { permission } from '../directives/permission'
import ProfileView from '../views/profile/ProfileView.vue'
import { updateCurrentUser } from '../api/user'
import { useUserStore } from '../stores/user'

vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn(() =>
    Promise.resolve({
      data: {
        userId: 1001,
        username: 'chenyt',
        nickname: '陈雨桐',
        email: 'chenyt@example.com',
        phone: '13800138001',
        departmentName: '产品研发中心',
        permissions: ['user:me:get', 'user:me:update'],
      },
    }),
  ),
  updateCurrentUser: vi.fn(() =>
    Promise.resolve({
      data: {
        userId: 1001,
        username: 'chenyt',
        nickname: '陈小雨',
        email: 'rain@example.com',
        phone: '13900139000',
        departmentName: '产品研发中心',
        permissions: ['user:me:get', 'user:me:update'],
      },
    }),
  ),
}))

function mountProfile() {
  const pinia = createPinia()
  setActivePinia(pinia)

  return mount(ProfileView, {
    global: {
      plugins: [pinia],
      directives: { permission },
      stubs: {
        AppButton: {
          emits: ['click'],
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        AppCard: { template: '<section><slot /></section>' },
        AppDialog: {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
        },
        AppInput: {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
        ElForm: { template: '<form><slot /></form>' },
        ElFormItem: { template: '<label><slot /></label>' },
      },
    },
  })
}

describe('ProfileView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('allows the signed-in user to update their nickname, email, and phone', async () => {
    const wrapper = mountProfile()
    await flushPromises()

    const editButton = wrapper.findAll('button').find((button) => button.text() === '编辑资料')
    expect(editButton?.exists()).toBe(true)

    await editButton.trigger('click')
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('陈小雨')
    await inputs[1].setValue('rain@example.com')
    await inputs[2].setValue('13900139000')

    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存修改')
    await saveButton.trigger('click')
    await flushPromises()

    expect(updateCurrentUser).toHaveBeenCalledWith({
      nickname: '陈小雨',
      email: 'rain@example.com',
      phone: '13900139000',
    })

    expect(useUserStore().user).toMatchObject({
      name: '陈小雨',
      email: 'rain@example.com',
      phone: '13900139000',
    })
  })
})
