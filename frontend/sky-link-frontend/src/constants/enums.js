export const userStatusOptions = [
  { value: 'active', label: '启用', tone: 'success' },
  { value: 'pending', label: '待激活', tone: 'warning' },
  { value: 'disabled', label: '停用', tone: 'danger' },
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

/**
 * 转换函数，将枚举选项数组转换为映射对象，便于根据 value 快速查找对应的枚举项
 * @param {Array} options
 * @returns {Object} 映射对象，键为 value，值为对应的枚举项
 * 结果示例：
 * {
 *   active: { value: 'active', label: '启用', tone: 'success' },
 *   pending: { value: 'pending', label: '待激活', tone: 'warning' },
 *   disabled: { value: 'disabled', label: '停用', tone: 'danger' },
 * }
 */
function createEnumMap(options) {
  return options.reduce((result, item) => {
    result[item.value] = item
    return result
  }, {})
}

export const userStatusMap = createEnumMap(userStatusOptions)
export const taskStatusMap = createEnumMap(taskStatusOptions)
export const noticeTypeMap = createEnumMap(noticeTypeOptions)
