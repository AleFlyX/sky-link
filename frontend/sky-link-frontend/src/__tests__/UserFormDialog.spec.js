import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

import UserFormDialog from '../views/users/components/UserFormDialog.vue'

function mountUserForm(formData) {
  return mount(UserFormDialog, {
    props: {
      modelValue: true,
      title: '添加用户',
      formData,
    },
    global: {
      plugins: [ElementPlus],
      stubs: {
        AppButton: {
          emits: ['click'],
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        AppDialog: {
          props: ['modelValue'],
          template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
        },
        AppInput: {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template:
            '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
      },
    },
  })
}

describe('UserFormDialog', () => {
  it('does not submit invalid new-user data', async () => {
    const wrapper = mountUserForm({
      username: 'a',
      password: 'password',
      nickname: '',
      email: 'not-an-email',
      phone: '123',
      departmentId: '',
      status: 1,
      roleIds: [],
    })

    const submitButton = wrapper.findAll('button').find((button) => button.text() === '创建用户')
    await submitButton.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('submits valid new-user data', async () => {
    const formData = {
      username: 'chenyt',
      password: 'Abc123456',
      nickname: '陈雨桐',
      email: 'chenyt@example.com',
      phone: '13800138001',
      departmentId: 201,
      status: 1,
      roleIds: [1],
    }
    const wrapper = mountUserForm(formData)

    const submitButton = wrapper.findAll('button').find((button) => button.text() === '创建用户')
    await submitButton.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')).toEqual([[formData]])
  })
})
