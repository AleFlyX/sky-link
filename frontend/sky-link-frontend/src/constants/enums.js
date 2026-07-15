export const userStatusOptions = [
  { value: 1, label: '启用', tone: 'success' },
  { value: 0, label: '禁用', tone: 'danger' },
]

export const taskStatusOptions = [
  { value: 'todo', label: '待开始', tone: 'info' },
  { value: 'doing', label: '进行中', tone: 'primary' },
  { value: 'done', label: '已完成', tone: 'success' },
  { value: 'blocked', label: '已阻塞', tone: 'danger' },
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
