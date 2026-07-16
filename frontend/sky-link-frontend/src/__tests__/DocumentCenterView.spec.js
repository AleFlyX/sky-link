import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent } from 'vue'

const testState = vi.hoisted(() => ({
  getDocuments: vi.fn(async () => ({
    data: {
      records: [
        {
          id: 10,
          title: 'Project plan',
          author: '陈雨桐',
          status: 'archived',
          updatedAt: '刚刚',
          permission: 'read',
          creatorId: 1001,
        },
      ],
    },
    source: 'remote',
    degraded: false,
  })),
  saveDocument: vi.fn(async () => ({ source: 'remote', degraded: false })),
  getDocumentPermissions: vi.fn(async () => ({ data: { users: [], groups: [] } })),
  setDocumentPermission: vi.fn(async () => ({ source: 'remote', degraded: false })),
  removeDocumentPermission: vi.fn(async () => ({ source: 'remote', degraded: false })),
  isDemoMode: vi.fn(() => false),
}))

vi.mock('../api/workspace', () => ({
  getDocuments: testState.getDocuments,
  saveDocument: testState.saveDocument,
  isDemoMode: testState.isDemoMode,
}))

vi.mock('../api/document', () => ({
  getDocumentPermissions: testState.getDocumentPermissions,
  setDocumentPermission: testState.setDocumentPermission,
  removeDocumentPermission: testState.removeDocumentPermission,
}))

import { permission } from '../directives/permission'
import { useUserStore } from '../stores/user'
import DocumentCenterView from '../views/documents/DocumentCenterView.vue'

const AppButtonStub = defineComponent({
  template: '<button type="button"><slot /></button>',
})

const AppCardStub = defineComponent({
  template: '<section class="app-card"><slot /></section>',
})

const AppDataTableStub = defineComponent({
  props: {
    rows: {
      type: Array,
      default: () => [],
    },
  },
  template: `
    <div class="data-table">
      <div v-for="row in rows" :key="row.id" class="data-row">
        <slot name="title" :value="row.title" :row="row" />
        <slot name="status" :value="row.status" />
        <slot name="actions" :row="row" />
      </div>
    </div>
  `,
})

const AppFormDialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    formData: {
      type: Object,
      default: () => ({}),
    },
    fields: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['submit', 'update:modelValue'],
  template: '<div v-if="modelValue" class="app-form-dialog"><slot /></div>',
})

const AppInputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue', 'keyup.enter'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
})

const AppPaginationStub = defineComponent({
  template: '<div class="pagination" />',
})

const AppStatusTagStub = defineComponent({
  props: {
    label: {
      type: String,
      default: '',
    },
  },
  template: '<span class="status-tag">{{ label }}</span>',
})

const ElDialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue'],
  template: `
    <div v-if="modelValue" class="el-dialog">
      <div class="el-dialog__body"><slot /></div>
      <div class="el-dialog__footer"><slot name="footer" /></div>
    </div>
  `,
})

const ElOptionStub = defineComponent({
  props: {
    value: {
      type: String,
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
  },
  template: '<option :value="value">{{ label }}</option>',
})

const ElSelectStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue', 'change'],
  template: `
    <select
      class="status-form__select"
      :disabled="disabled"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value); $emit('change', $event.target.value)"
    >
      <slot />
    </select>
  `,
})

const ElTableStub = defineComponent({
  template: '<div class="el-table"><slot /></div>',
})

const ElTableColumnStub = defineComponent({
  template: '<div class="el-table-column"><slot /></div>',
})

const ElButtonStub = defineComponent({
  template: '<button type="button"><slot /></button>',
})

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const userStore = useUserStore()
  userStore.setUser({
    userId: 1001,
    name: '陈雨桐',
    permissions: [
      'document:create',
      'document:update',
      'document:permission:list',
      'document:permission:user:set',
      'document:permission:user:delete',
    ],
  })

  return mount(DocumentCenterView, {
    global: {
      directives: { permission },
      plugins: [pinia],
      stubs: {
        AppButton: AppButtonStub,
        AppCard: AppCardStub,
        AppDataTable: AppDataTableStub,
        AppFormDialog: AppFormDialogStub,
        AppInput: AppInputStub,
        AppPagination: AppPaginationStub,
        AppStatusTag: AppStatusTagStub,
        ElAlert: { template: '<div><slot /></div>' },
        ElButton: ElButtonStub,
        ElDialog: ElDialogStub,
        ElOption: ElOptionStub,
        ElSelect: ElSelectStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
      },
    },
  })
}

describe('DocumentCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('shows status management for an archived document creator and restores it from the list page', async () => {
    const wrapper = mountView()

    await flushPromises()

    const statusButton = wrapper.findAll('button').find((button) => button.text() === '状态管理')
    expect(statusButton).toBeTruthy()

    await statusButton.trigger('click')
    await flushPromises()

    expect(wrapper.find('select.status-form__select').exists()).toBe(true)
    expect(wrapper.find('select.status-form__select').element.value).toBe('archived')

    await wrapper.find('select.status-form__select').setValue('team')
    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存')
    expect(saveButton).toBeTruthy()

    await saveButton.trigger('click')
    await flushPromises()

    expect(testState.saveDocument).toHaveBeenCalledWith({ status: 'team' }, 10)
  })
})
