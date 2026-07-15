import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createTask,
  getDepartmentMembers,
  getTasks,
  getUsers,
  isDemoMode,
  updateTaskStatus,
} from '../../../api/workspace'
import { normalizeTaskStatus, toTaskStatusApiValue } from '../../../constants/enums'
import { useUserStore } from '../../../stores/user'

const pageSize = 5

const priorityMap = {
  1: '低',
  2: '中',
  3: '高',
}

const priorityOptions = [
  { value: 1, label: '低' },
  { value: 2, label: '中' },
  { value: 3, label: '高' },
]

const taskFormInitialData = {
  title: '',
  content: '',
  executorId: '',
  priority: 2,
  deadline: '',
}

function toSearchText(value) {
  return String(value ?? '').toLowerCase()
}

function formatTaskPriority(priority) {
  return priorityMap[priority] || priority || '-'
}

function formatTaskDate(value) {
  if (!value) return '待定'

  return String(value).replace('T', ' ').slice(0, 16)
}

function unwrapData(response) {
  return response?.data ?? response ?? {}
}

function normalizePage(data) {
  if (Array.isArray(data)) {
    return { records: data, total: data.length, page: 1, size: data.length }
  }

  return {
    records: data?.records || [],
    total: data?.total || 0,
    page: data?.page || 1,
    size: data?.size || 0,
  }
}

function normalizeUserOption(item) {
  const userId = item.userId ?? item.id
  const username = item.username ?? item.account ?? ''
  const nickname = item.nickname ?? item.name ?? ''
  const departmentName = item.departmentName ?? item.department ?? ''

  return {
    value: userId,
    label: `${nickname || username || `用户#${userId}`}${departmentName ? ` · ${departmentName}` : ''}`,
  }
}

function normalizeDeadline(value) {
  if (!value) return undefined
  if (value instanceof Date) return value.toISOString()

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toISOString()
}

function getNextStatusAction(row) {
  const currentStatus = normalizeTaskStatus(row.status)
  const actionMap = {
    todo: { label: '开始', nextStatus: 'doing' },
    doing: { label: '完成', nextStatus: 'done' },
    done: { label: '重开', nextStatus: 'doing' },
  }

  return actionMap[currentStatus] || null
}

function normalizeTaskRow(item) {
  const executor = item.executor || {}

  return {
    ...item,
    id: item.id ?? item.taskId,
    assignee: item.assignee ?? executor.nickname ?? executor.username ?? '-',
    priority: formatTaskPriority(item.priority),
    status: normalizeTaskStatus(item.status),
    dueDate: item.dueDate ?? formatTaskDate(item.deadline),
  }
}

export function useTaskList() {
  const userStore = useUserStore()

  const keyword = ref('')
  const status = ref('')
  const page = ref(1)
  const dialogVisible = ref(false)
  const rows = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const demoData = ref(isDemoMode())
  const assigneeOptions = ref([])
  const assigneeLoading = ref(false)
  const assigneeError = ref('')
  const updatingTaskId = ref(null)

  const columns = [
    { key: 'title', label: '任务标题' },
    { key: 'assignee', label: '负责人' },
    { key: 'priority', label: '优先级' },
    { key: 'status', label: '任务状态', slot: 'status' },
    { key: 'dueDate', label: '截止日期' },
    { key: 'actions', label: '操作', slot: 'actions', width: '120px', align: 'right' },
  ]

  const taskFormFields = computed(() => [
    { key: 'title', label: '任务标题', required: true },
    { key: 'content', label: '任务说明', type: 'textarea', maxlength: 1000, showWordLimit: true },
    {
      key: 'executorId',
      label: '负责人',
      type: 'select',
      options: assigneeOptions.value,
      required: true,
      clearable: true,
      disabled: assigneeLoading.value || assigneeOptions.value.length === 0,
      placeholder: assigneeLoading.value ? '正在加载部门成员' : '选择部门成员',
      description: assigneeError.value || '负责人优先从当前用户所在部门成员中选择',
    },
    { key: 'priority', label: '优先级', type: 'select', options: priorityOptions, required: true },
    { key: 'deadline', label: '截止时间', type: 'datetime', clearable: true },
  ])

  const filteredRows = computed(() =>
    rows.value.filter((item) => {
      const matchKeyword = [item.title, item.assignee, item.priority].some((text) =>
        toSearchText(text).includes(toSearchText(keyword.value)),
      )
      const matchStatus = !status.value || normalizeTaskStatus(item.status) === status.value
      return matchKeyword && matchStatus
    }),
  )

  const pagedRows = computed(() => {
    const start = (page.value - 1) * pageSize
    return filteredRows.value.slice(start, start + pageSize)
  })

  watch([keyword, status], () => {
    page.value = 1
  })

  async function loadData() {
    loading.value = true
    loadError.value = ''

    try {
      const result = await getTasks({ page: 1, size: 100 })
      const data = result.data ?? {}
      rows.value = (data.records || []).map(normalizeTaskRow)
      demoData.value = result.source === 'demo'

      if (result.degraded) {
        loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
      }
    } catch (error) {
      rows.value = []
      loadError.value = error.message || '任务加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadAssigneeOptions() {
    assigneeLoading.value = true
    assigneeError.value = ''

    try {
      const departmentId = userStore.user?.departmentId
      const response = departmentId
        ? await getDepartmentMembers(departmentId, { page: 1, size: 500 })
        : await getUsers({ page: 1, size: 500 })
      const data = unwrapData(response)
      assigneeOptions.value = normalizePage(data)
        .records
        .map(normalizeUserOption)
        .filter((item) => item.value != null)
    } catch (error) {
      assigneeOptions.value = []
      assigneeError.value = error.message || '部门成员加载失败'
    } finally {
      assigneeLoading.value = false
    }
  }

  async function handleSubmit(form) {
    const payload = {
      title: form.title,
      content: form.content || undefined,
      executorId: form.executorId === '' ? undefined : Number(form.executorId),
      priority: Number(form.priority || 2),
      deadline: normalizeDeadline(form.deadline),
    }
    const result = await createTask(payload)
    ElMessage[result.degraded ? 'warning' : 'success'](
      result.degraded ? '接口暂不可用，已保存到演示数据' : '任务已创建',
    )
    dialogVisible.value = false
    await loadData()
  }

  async function handleStatusUpdate(row) {
    const action = getNextStatusAction(row)
    if (!action) return

    updatingTaskId.value = row.id
    try {
      const result = await updateTaskStatus(row.id, toTaskStatusApiValue(action.nextStatus))
      ElMessage[result.degraded ? 'warning' : 'success'](
        result.degraded ? '接口暂不可用，已在演示数据中更新状态' : '任务状态已更新',
      )
      await loadData()
    } finally {
      updatingTaskId.value = null
    }
  }

  onMounted(() => {
    loadData()
    loadAssigneeOptions()
  })

  return {
    columns,
    demoData,
    dialogVisible,
    filteredRows,
    getNextStatusAction,
    handleStatusUpdate,
    handleSubmit,
    keyword,
    loadData,
    loadError,
    loading,
    page,
    pageSize,
    pagedRows,
    rows,
    status,
    taskFormFields,
    taskFormInitialData,
    updatingTaskId,
  }
}
