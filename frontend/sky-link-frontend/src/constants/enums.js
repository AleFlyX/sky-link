export const userStatusOptions = [
  { value: 1, label: '启用', tone: 'success' },
  { value: 0, label: '禁用', tone: 'danger' },
]

export const taskStatusOptions = [
  { value: 'todo', label: '未开始', tone: 'info' },
  { value: 'doing', label: '进行中', tone: 'primary' },
  { value: 'done', label: '已完成', tone: 'success' },
  { value: 'cancelled', label: '已取消', tone: 'default' },
]

export const noticeTypeOptions = [
  { value: 'system', label: '系统公告', tone: 'primary' },
  { value: 'project', label: '项目通知', tone: 'success' },
  { value: 'alert', label: '紧急提醒', tone: 'danger' },
]

function createEnumMap(options) {
  return options.reduce((result, item) => {
    result[item.value] = item
    return result
  }, {})
}

export const userStatusMap = createEnumMap(userStatusOptions)
export const taskStatusMap = createEnumMap(taskStatusOptions)
export const noticeTypeMap = createEnumMap(noticeTypeOptions)

const taskStatusAliases = {
  未开始: 'todo',
  待开始: 'todo',
  进行中: 'doing',
  已完成: 'done',
  已取消: 'cancelled',
}

const taskStatusApiValues = {
  todo: '未开始',
  doing: '进行中',
  done: '已完成',
}

export function normalizeTaskStatus(status) {
  if (status == null || status === '') return ''

  const value = String(status).trim()
  return taskStatusMap[value] ? value : taskStatusAliases[value] || value
}

export function getTaskStatusMeta(status) {
  const normalizedStatus = normalizeTaskStatus(status)
  return (
    taskStatusMap[normalizedStatus] || { label: status ? String(status) : '未知', tone: 'info' }
  )
}

export function toTaskStatusApiValue(status) {
  const normalizedStatus = normalizeTaskStatus(status)
  return taskStatusApiValues[normalizedStatus] || String(status || '')
}
