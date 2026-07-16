import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, nextTick } from 'vue'

import { createTask, getDepartmentMembers, getUsers, updateTaskStatus } from '../api/workspace'
import { permission } from '../directives/permission'
import { useUserStore } from '../stores/user'
import TaskListView from '../views/tasks/TaskListView.vue'

vi.mock('../api/workspace', () => ({
  createTask: vi.fn(() => Promise.resolve({ source: 'remote', degraded: false })),
  getDepartmentMembers: vi.fn(() =>
    Promise.resolve({
      data: {
        records: [
          {
            userId: 1003,
            username: 'wangjy',
            nickname: '王嘉怡',
            departmentId: 201,
            departmentName: '产品研发中心',
          },
        ],
      },
      source: 'remote',
      degraded: false,
    }),
  ),
  getTasks: vi.fn(() =>
    Promise.resolve({
      data: {
        records: [
          {
            taskId: 1,
            title: '联调任务列表',
            executor: { nickname: '李明浩', username: 'limh' },
            priority: 2,
            status: '未开始',
            deadline: '2026-07-20T18:00:00',
          },
        ],
      },
      source: 'remote',
      degraded: false,
    }),
  ),
  getUsers: vi.fn(() => Promise.resolve({ data: { records: [] }, source: 'remote', degraded: false })),
  isDemoMode: vi.fn(() => false),
  updateTaskStatus: vi.fn(() => Promise.resolve({ source: 'remote', degraded: false })),
}))

const FormDialogStub = defineComponent({
  props: {
    fields: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['submit'],
  template: `
    <div>
      <div v-for="field in fields" :key="field.key">
        <span>{{ field.label }}</span>
        <span v-for="option in field.options || []" :key="option.value">{{ option.label }}</span>
      </div>
      <button
        type="button"
        class="submit-task"
        @click="$emit('submit', {
          title: '部门任务',
          content: '请完成接口联调',
          executorId: 1003,
          priority: 3,
          deadline: new Date('2026-07-20T10:00:00Z')
        })"
      >
        submit
      </button>
    </div>
  `,
})

function mountTaskList(permissions = ['task:create'], userOverrides = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const userStore = useUserStore()
  userStore.setUser({ userId: 1001, departmentId: 201, permissions, ...userOverrides })

  return mount(TaskListView, {
    global: {
      directives: { permission },
      plugins: [pinia],
      stubs: {
        AppButton: { template: '<button type="button" v-bind="$attrs"><slot /></button>' },
        AppCard: { template: '<section><slot /></section>' },
        AppFormDialog: FormDialogStub,
        AppInput: { template: '<input />' },
        AppPagination: true,
        ElAlert: { props: ['title'], template: '<div>{{ title }}<slot /></div>' },
        ElOption: true,
        ElSelect: { template: '<select><slot /></select>' },
        ElSkeleton: { template: '<div />' },
      },
    },
  })
}

describe('TaskListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('renders task statuses returned by the backend contract', async () => {
    const wrapper = mountTaskList()

    await flushPromises()

    expect(wrapper.text()).toContain('未开始')
  })

  it('uses current department members as task assignee options and submits executorId', async () => {
    const wrapper = mountTaskList()

    await flushPromises()

    expect(getDepartmentMembers).toHaveBeenCalledWith(201, { page: 1, size: 500 })
    expect(getUsers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('负责人')
    expect(wrapper.text()).toContain('王嘉怡 · 产品研发中心')

    await wrapper.find('.submit-task').trigger('click')

    expect(createTask).toHaveBeenCalledWith({
      title: '部门任务',
      content: '请完成接口联调',
      executorId: 1003,
      priority: 3,
      deadline: '2026-07-20T10:00:00.000Z',
    })
  })

  it('disables task creation when the current user has no department', async () => {
    const wrapper = mountTaskList(['task:create'], { departmentId: null })

    await flushPromises()

    expect(getDepartmentMembers).not.toHaveBeenCalled()
    expect(getUsers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前账号未分配部门，任务只能指派给同部门成员')

    const createButton = wrapper.findAll('button').find((button) => button.text() === '新建任务')
    expect(createButton.attributes('disabled')).toBeDefined()
  })

  it('hides create task action without task:create permission', async () => {
    const wrapper = mountTaskList(['task:list'])

    await nextTick()

    expect(wrapper.findAll('button').find((button) => button.text() === '新建任务').element.style.display).toBe('none')
  })

  it('updates task status to the next backend status value', async () => {
    const wrapper = mountTaskList(['task:create', 'task:status:update'])

    await flushPromises()

    const startButton = wrapper.findAll('button').find((button) => button.text() === '开始')
    expect(startButton.exists()).toBe(true)

    await startButton.trigger('click')

    expect(updateTaskStatus).toHaveBeenCalledWith(1, '进行中')
  })
})
