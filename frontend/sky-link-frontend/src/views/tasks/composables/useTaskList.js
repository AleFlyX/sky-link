import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createTask,
  getDepartmentMembers,
  getTasks,
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
  const total = ref(0)
  const dialogVisible = ref(false)
  const rows = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const demoData = ref(isDemoMode())
  const assigneeOptions = ref([])
  const assigneeLoading = ref(false)
  const assigneeError = ref('')
  const updatingTaskId = ref(null)
  const currentDepartmentId = computed(() => {
    const value = userStore.user?.departmentId
    if (value === '' || value === null || value === undefined) {
      return null
    }

    const normalized = Number(value)
    return Number.isFinite(normalized) ? normalized : null
  })

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
      description: assigneeError.value || '只能为当前部门成员分配任务',
    },
    { key: 'priority', label: '优先级', type: 'select', options: priorityOptions, required: true },
    { key: 'deadline', label: '截止时间', type: 'datetime', clearable: true },
  ])
  const taskCreationNotice = computed(() => {
    if (!currentDepartmentId.value) {
      return '当前账号未分配部门，任务只能指派给同部门成员，请先加入部门后再创建任务'
    }
    if (assigneeError.value) {
      return assigneeError.value
    }
    return ''
  })
  const taskCreationDisabled = computed(
    () => assigneeLoading.value || Boolean(taskCreationNotice.value),
  )

  async function loadData(targetPage = page.value) {
    loading.value = true
    loadError.value = ''

    try {
      const result = await getTasks({
        page: targetPage,
        size: pageSize,
        keyword: keyword.value.trim() || undefined,
        status: status.value || undefined,
      })
      const data = result.data ?? {}
      rows.value = (data.records || []).map(normalizeTaskRow)
      total.value = data.total || 0
      page.value = data.page || targetPage
      demoData.value = result.source === 'demo'

      if (result.degraded) {
        loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
      }
    } catch (error) {
      rows.value = []
      total.value = 0
      loadError.value = error.message || '任务加载失败'
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    page.value = 1
    loadData(1)
  }

  function handleReset() {
    keyword.value = ''
    status.value = ''
    page.value = 1
    loadData(1)
  }

  function handlePageChange(nextPage) {
    page.value = nextPage
    loadData(nextPage)
  }

  async function loadAssigneeOptions() {
    assigneeLoading.value = true
    assigneeError.value = ''

    try {
      if (!currentDepartmentId.value) {
        assigneeOptions.value = []
        return
      }

      const response = await getDepartmentMembers(currentDepartmentId.value, { page: 1, size: 500 })
      const data = unwrapData(response)
      assigneeOptions.value = normalizePage(data)
        .records.map(normalizeUserOption)
        .filter((item) => item.value != null)
      assigneeError.value = assigneeOptions.value.length
        ? ''
        : '当前部门暂无可分配成员，请先维护部门成员后再创建任务'
    } catch (error) {
      assigneeOptions.value = []
      assigneeError.value = error.message || '部门成员加载失败，暂时无法创建任务'
    } finally {
      assigneeLoading.value = false
    }
  }

  async function handleSubmit(form) {
    if (taskCreationDisabled.value) {
      ElMessage.warning(taskCreationNotice.value || '当前无法创建任务')
      return
    }

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
    getNextStatusAction,
    handlePageChange,
    handleReset,
    handleSearch,
    handleStatusUpdate,
    handleSubmit,
    keyword,
    loadData,
    loadError,
    loading,
    page,
    pageSize,
    rows,
    status,
    taskCreationDisabled,
    taskCreationNotice,
    taskFormFields,
    taskFormInitialData,
    total,
    updatingTaskId,
  }
}
