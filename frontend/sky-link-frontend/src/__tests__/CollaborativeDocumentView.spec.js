import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, ref } from 'vue'

const testState = vi.hoisted(() => ({
  updateDocument: vi.fn(async () => ({ source: 'remote', degraded: false })),
  getDocument: vi.fn(async () => ({
    data: {
      documentId: 10,
      title: 'Project plan',
      content: '# Plan',
      status: 'archived',
      creatorId: 1001,
      creatorName: '陈雨桐',
      permission: 'read',
    },
  })),
  pushSpy: vi.fn(),
  connectSpy: vi.fn(async () => {}),
  sessionPermission: null,
  sessionEditable: null,
  pageAgentCtor: vi.fn(),
  pageAgentShow: vi.fn(),
  pageAgentDispose: vi.fn(),
}))

vi.mock('../api/document', () => ({
  getDocument: testState.getDocument,
  updateDocument: testState.updateDocument,
  createCollaborationTicket: vi.fn(),
}))

vi.mock('../views/documents/composables/useCollaborationSession', () => ({
  useCollaborationSession: vi.fn(() => ({
    ydoc: {},
    provider: ref(null),
    status: ref('synced'),
    permission: testState.sessionPermission,
    editable: testState.sessionEditable,
    error: ref(''),
    savedAt: ref(null),
    connect: testState.connectSpy,
    disconnect: vi.fn(),
  })),
}))

vi.mock('page-agent', () => ({
  PageAgent: class {
    constructor(config) {
      testState.pageAgentCtor(config)
      this.panel = {
        show: testState.pageAgentShow,
        dispose: testState.pageAgentDispose,
      }
    }
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { documentId: '10' } }),
  useRouter: () => ({ push: testState.pushSpy }),
}))

vi.mock('@tiptap/core', () => ({
  Editor: class {
    constructor(configuration) {
      this.configuration = configuration
      this.editable = configuration.editable
    }
    setEditable(value) {
      this.editable = value
    }
    destroy() {}
    chain() {
      const api = {
        focus() {
          return api
        },
        insertTable() {
          return api
        },
        deleteTable() {
          return api
        },
        setImage() {
          return api
        },
        run() {
          return api
        },
      }
      return api
    }
    isActive() {
      return false
    }
    getText() {
      return ''
    }
  },
}))

vi.mock('@tiptap/vue-3', () => ({
  EditorContent: { template: '<div class="editor-content" />' },
}))

vi.mock('@tiptap/starter-kit', () => ({ default: { configure: () => ({}) } }))
vi.mock('@tiptap/extension-image', () => ({ Image: { configure: () => ({}) } }))
vi.mock('@tiptap/extension-table', () => ({
  Table: { configure: () => ({}) },
  TableCell: {},
  TableHeader: {},
  TableRow: {},
}))
vi.mock('@tiptap/extension-collaboration', () => ({ default: { configure: () => ({}) } }))
vi.mock('@tiptap/extension-collaboration-caret', () => ({ default: { configure: () => ({}) } }))

import { useUserStore } from '../stores/user'

const AppInputStub = defineComponent({
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
  emits: ['update:modelValue', 'blur'],
  template: `
    <input
      :disabled="disabled"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="$emit('blur')"
    />
  `,
})

const AppButtonStub = defineComponent({
  template: '<button type="button"><slot /></button>',
})

const AppCardStub = defineComponent({
  props: {
    bodyClass: {
      type: [String, Array, Object],
      default: '',
    },
  },
  template: '<section class="app-card"><div :class="bodyClass"><slot /></div></section>',
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
      <div class="el-dialog__body">
        <slot />
      </div>
      <div class="el-dialog__footer">
        <slot name="footer" />
      </div>
    </div>
  `,
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
      class="visibility-select"
      :disabled="disabled"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value); $emit('change', $event.target.value)"
    >
      <slot />
    </select>
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

async function mountView() {
  const { default: CollaborativeDocumentView } =
    await import('../views/documents/CollaborativeDocumentView.vue')
  const pinia = createPinia()
  setActivePinia(pinia)
  const userStore = useUserStore()
  userStore.setUser({
    userId: 1001,
    name: '陈雨桐',
    account: 'chenyt',
    roles: [],
    permissions: [],
  })

  return mount(CollaborativeDocumentView, {
    global: {
      plugins: [pinia],
      stubs: {
        AppButton: AppButtonStub,
        AppCard: AppCardStub,
        AppInput: AppInputStub,
        AppStatusTag: AppStatusTagStub,
        ElAlert: { template: '<div><slot /></div>' },
        ElDialog: ElDialogStub,
        ElOption: ElOptionStub,
        ElSelect: ElSelectStub,
        ElSkeleton: { template: '<div class="skeleton" />' },
        EditorContent: { template: '<div class="editor-content" />' },
      },
    },
  })
}

describe('CollaborativeDocumentView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    testState.sessionPermission = ref('read')
    testState.sessionEditable = ref(false)
    window.localStorage.clear()
    globalThis.ElMessage = {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    }
  })

  it('lets the creator restore the document visibility range from the editor page', async () => {
    const wrapper = await mountView()

    await flushPromises()

    const openButton = wrapper.findAll('button').find((button) => button.text() === '修改可见范围')
    expect(openButton).toBeTruthy()

    await openButton.trigger('click')
    await flushPromises()

    expect(wrapper.find('select.visibility-select').exists()).toBe(true)
    expect(wrapper.find('select.visibility-select').element.value).toBe('archived')

    await wrapper.find('select.visibility-select').setValue('team')
    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存')
    expect(saveButton).toBeTruthy()
    await saveButton.trigger('click')
    await flushPromises()

    expect(testState.updateDocument).toHaveBeenCalledWith(10, { status: 'team' })
  })

  it('opens qwen page agent from the editor sidebar and disposes it on unmount', async () => {
    const wrapper = await mountView()

    await flushPromises()

    const agentButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '打开 Qwen Page Agent')
    expect(agentButton).toBeTruthy()

    await agentButton.trigger('click')
    await flushPromises()

    expect(testState.pageAgentCtor).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'qwen3.5-plus',
        baseURL: 'https://page-ag-testing-ohftxirgbn.cn-shanghai.fcapp.run',
        apiKey: 'NA',
        language: 'zh-CN',
      }),
    )
    expect(testState.pageAgentShow).toHaveBeenCalledTimes(1)

    wrapper.unmount()

    expect(testState.pageAgentDispose).toHaveBeenCalledTimes(1)
  })
})
