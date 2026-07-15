import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, nextTick } from 'vue'

import { permission } from '../directives/permission'
import { useUserStore } from '../stores/user'
import UserListView from '../views/users/UserListView.vue'
import { createUser as createUserApi } from '../api/user'

vi.mock('../api/department', () => ({
  getDepartments: vi.fn(() =>
    Promise.resolve({
      data: [
        { departmentId: 201, departmentName: '产品研发中心' },
      ],
    }),
  ),
}))

vi.mock('../api/role', () => ({
  getRoles: vi.fn(() =>
    Promise.resolve({
      data: {
        records: [
          { roleId: 1, roleCode: 'ROLE_USER', roleName: '普通成员' },
          { roleId: 2, roleCode: 'ROLE_ADMIN', roleName: '管理员' },
        ],
      },
    }),
  ),
}))

vi.mock('../api/user', () => ({
  createUser: vi.fn(() => Promise.resolve({ data: {} })),
  assignUserRoles: vi.fn(() => Promise.resolve({ data: [] })),
  deleteUser: vi.fn(() => Promise.resolve({ data: {} })),
  getUser: vi.fn(() =>
    Promise.resolve({
      data: {
        userId: 1001,
        username: 'chenyt',
        nickname: '陈雨桐',
        departmentName: '产品研发中心',
        status: 1,
        roles: [],
      },
    }),
  ),
  getUsers: vi.fn(() =>
    Promise.resolve({
      data: {
        records: [],
        total: 0,
        page: 1,
      },
    }),
  ),
  updateUserStatus: vi.fn(() => Promise.resolve({ data: {} })),
}))

const UserFormDialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['submit', 'update:modelValue'],
  template: `
    <div v-if="modelValue" class="user-form-dialog-stub">
      <button
        type="button"
        class="user-form-dialog-stub__submit"
        @click="$emit('submit', {
          username: 'newuser',
          password: 'Abc123456',
          nickname: '新用户',
          email: 'newuser@example.com',
          phone: '13800138001',
          departmentId: '',
          status: 1,
          roleIds: [1]
        })"
      >
        submit
      </button>
    </div>
  `,
})

function mountUserList(permissions = ['user:list', 'user:create']) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const userStore = useUserStore()
  userStore.setUser({ userId: 1001, permissions })

  return mount(UserListView, {
    global: {
      directives: { permission },
      plugins: [pinia],
      stubs: {
        AppButton: { template: '<button type="button" @click="$emit(\'click\')"><slot /></button>' },
        AppCard: { template: '<section><slot /></section>' },
        AppDataTable: { template: '<div><slot name="actions" :row="{}" /><slot /></div>' },
        AppDialog: { template: '<div v-if="$attrs.modelValue"><slot /><slot name="footer" /></div>' },
        AppPagination: true,
        AppStatusTag: { template: '<span><slot /></span>' },
        UserFormDialog: UserFormDialogStub,
        ElAlert: { template: '<div />' },
        ElInput: { template: '<input />' },
        ElOption: true,
        ElSelect: { template: '<select><slot /></select>' },
        ElSkeleton: { template: '<div />' },
      },
    },
  })
}

describe('UserListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('shows the add user action with permission and opens the create dialog', async () => {
    const wrapper = mountUserList()

    await flushPromises()

    const addButton = wrapper.findAll('button').find((button) => button.text() === '添加用户')
    expect(addButton?.exists()).toBe(true)

    await addButton.trigger('click')
    await nextTick()

    expect(wrapper.find('.user-form-dialog-stub').exists()).toBe(true)

    await wrapper.find('.user-form-dialog-stub__submit').trigger('click')
    await flushPromises()

    expect(createUserApi).toHaveBeenCalledWith({
      username: 'newuser',
      password: 'Abc123456',
      nickname: '新用户',
      email: 'newuser@example.com',
      phone: '13800138001',
      departmentId: undefined,
      status: 1,
      roleIds: [1],
    })
  })
})
