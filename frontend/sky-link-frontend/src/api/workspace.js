import * as authApi from './auth'
import * as departmentApi from './department'
import * as documentApi from './document'
import * as fileApi from './file'
import * as friendApi from './friend'
import * as groupApi from './group'
import * as messageApi from './message'
import * as noticeApi from './notice'
import * as scheduleApi from './schedule'
import * as taskApi from './task'
import * as userApi from './user'
import {
  departments,
  files,
  notices,
  tasks,
  users,
} from '../mock/workspace'

const demoCurrentUser = {
  id: 1001,
  name: '陈雨桐',
  account: 'chenyt',
  department: '产品研发中心',
  roleLabel: '产品管理员',
}

const demoFriends = [
  { id: 601, userId: 1002, name: '李明浩', account: 'limh', department: '综合管理部', status: '在线', lastSeen: '刚刚' },
  { id: 602, userId: 1003, name: '王嘉怡', account: 'wangjy', department: '产品研发中心', status: '忙碌', lastSeen: '10 分钟前' },
  { id: 603, userId: 1005, name: '赵思涵', account: 'zhaosh', department: '综合管理部', status: '离线', lastSeen: '昨天 18:40' },
]

const demoGroups = [
  { id: 701, name: 'Day 3 联调小组', memberCount: 6, notice: '今天完成消息、文档和日程演示收口。', updatedAt: '今天 09:20' },
  { id: 702, name: '产品研发中心', memberCount: 18, notice: '需求、设计与研发同步。', updatedAt: '昨天 17:30' },
]

const demoSessions = [
  { id: 'single-1002', sessionType: 'single', targetId: 1002, targetName: '李明浩', lastTime: '10:32', lastMessage: '我已把部门名单同步好了。' },
  { id: 'group-701', sessionType: 'group', targetId: 701, targetName: 'Day 3 联调小组', lastTime: '09:48', lastMessage: '文档详情页可以开始验收了。' },
]

const demoMessages = {
  'single-1002': [
    { id: 801, senderId: 1002, senderName: '李明浩', content: '早上好，今天先联调消息和文档页面。', sentAt: '09:12' },
    { id: 802, senderId: 1001, senderName: '陈雨桐', content: '收到，我会把加载态和异常反馈一起补齐。', sentAt: '09:18' },
    { id: 803, senderId: 1002, senderName: '李明浩', content: '我已把部门名单同步好了。', sentAt: '10:32' },
  ],
  'group-701': [
    { id: 804, senderId: 1003, senderName: '王嘉怡', content: '任务筛选和分页已经可以演示。', sentAt: '09:40' },
    { id: 805, senderId: 1001, senderName: '陈雨桐', content: '文档详情页可以开始验收了。', sentAt: '09:48' },
  ],
}

const demoDocuments = [
  { id: 901, title: 'Day 3 全链路演示计划', summary: '从登录到管理页面的演示路径与验收记录。', status: 'team', updatedAt: '今天 10:20', author: '陈雨桐', content: '# Day 3 全链路演示计划\n\n完成登录、用户、文件、任务、公告，以及消息、文档、日程页面联调。' },
  { id: 902, title: '接口联调记录', summary: '记录后端接口状态、前端降级策略和待办事项。', status: 'private', updatedAt: '昨天 18:12', author: '李明浩', content: '# 接口联调记录\n\n当前后端接口尚未提供 Controller，前端通过统一数据服务保留可切换的演示数据。' },
]

const demoSchedules = [
  { id: 1001, title: '课程展示彩排', content: '走一遍登录、管理页和结果展示。', startTime: '2026-07-14 09:00', endTime: '2026-07-14 10:00', repeatType: 'none', owner: '陈雨桐' },
  { id: 1002, title: '后端接口联调', content: '确认用户、文件和任务接口返回结构。', startTime: '2026-07-14 14:00', endTime: '2026-07-14 15:30', repeatType: 'weekly', owner: '团队' },
]

const state = {
  users: [...users],
  departments: [...departments],
  files: [...files],
  tasks: [...tasks],
  notices: [...notices],
  friends: [...demoFriends],
  groups: [...demoGroups],
  sessions: [...demoSessions],
  messages: JSON.parse(JSON.stringify(demoMessages)),
  documents: [...demoDocuments],
  schedules: [...demoSchedules],
}

// 数据源配置: 是实际远程接口数据，还是本地模拟数据。可选值: "remote" | "mock"
const dataSource = import.meta.env.VITE_DATA_SOURCE || 'remote'
// 验证数据源配置是否合法
if (!['remote', 'mock'].includes(dataSource)) {
  throw new Error(`Invalid VITE_DATA_SOURCE: ${dataSource}. Expected "remote" or "mock".`)
}

const mockEnabled = dataSource === 'mock'

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function pageOf(records, page = 1, size = 5) {
  const start = (page - 1) * size
  return { total: records.length, page, size, records: clone(records.slice(start, start + size)) }
}

function demoResult(factory) {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve({ data: clone(factory()), source: 'demo', degraded: false }), 120)
  })
}

async function remoteOrDemo(remoteRequest, factory) {
  if (mockEnabled) {
    return demoResult(factory)
  }

  const payload = await remoteRequest()
  return { data: payload?.data ?? payload, source: 'remote', degraded: false }
}

function searchRecords(records, keyword, fields) {
  const value = String(keyword || '').trim().toLowerCase()
  if (!value) return records
  return records.filter((record) => fields.some((field) => String(record[field] ?? '').toLowerCase().includes(value)))
}

function messageTarget(sessionId) {
  const [sessionType, targetId] = String(sessionId).split('-')
  const id = Number(targetId)
  return sessionType === 'group' ? { groupId: id } : { receiverId: id }
}

export function isDemoMode() {
  return mockEnabled
}

export async function login(account, password) {
  if (!mockEnabled) {
    const payload = await authApi.login(account, password)
    return payload?.data ?? payload
  }
  return demoResult(() => ({ token: `demo-token-${Date.now()}`, expiresIn: 86400, userInfo: clone(demoCurrentUser) })).then((result) => result.data)
}

export async function register(data) {
  if (!mockEnabled) {
    const payload = await authApi.register(data)
    return payload?.data ?? payload
  }

  return demoResult(() => ({
    userId: Date.now(),
    username: data.username,
    nickname: data.nickname || data.username,
    email: data.email,
    phone: data.phone,
    createTime: new Date().toISOString(),
  })).then((result) => result.data)
}

export function getCurrentUser() {
  return remoteOrDemo(() => userApi.getCurrentUser(), () => demoCurrentUser)
}

export function getUsers({ page = 1, size = 5, keyword = '', status = '' } = {}) {
  return remoteOrDemo(() => userApi.getUsers({ page, size, keyword, status }), () => pageOf(searchRecords(state.users.filter((item) => !status || item.status === status), keyword, ['name', 'account', 'department']), page, size))
}

export function createUser(data) {
  return remoteOrDemo(() => userApi.createUser(data), () => {
    const item = { ...data, id: Date.now(), roles: ['普通成员'], status: data.status || 'active', updatedAt: '刚刚', phone: data.phone || '未填写' }
    state.users.unshift(item)
    return item
  })
}

export function getDepartments({ page = 1, size = 5, keyword = '' } = {}) {
  return remoteOrDemo(() => departmentApi.getDepartments({ page, size, keyword }), () => pageOf(searchRecords(state.departments, keyword, ['name', 'leader', 'roleScope']), page, size))
}

export function getFiles({ page = 1, size = 5, keyword = '' } = {}) {
  return remoteOrDemo(() => fileApi.getFiles({ page, size, keyword }), () => pageOf(searchRecords(state.files, keyword, ['name', 'category', 'owner']), page, size))
}

export function createFile(data) {
  return remoteOrDemo(() => fileApi.createFile(data), () => {
    const item = { ...data, id: Date.now(), owner: demoCurrentUser.name, size: '待上传', updatedAt: '刚刚' }
    state.files.unshift(item)
    return item
  })
}

export function getTasks({ page = 1, size = 5, keyword = '', status = '' } = {}) {
  return remoteOrDemo(() => taskApi.getTasks({ page, size, keyword, status }), () => pageOf(searchRecords(state.tasks.filter((item) => !status || item.status === status), keyword, ['title', 'assignee', 'priority']), page, size))
}

export function createTask(data) {
  return remoteOrDemo(() => taskApi.createTask(data), () => {
    const item = { ...data, id: Date.now(), priority: data.priority || '中', status: data.status || 'todo', dueDate: data.dueDate || '待定' }
    state.tasks.unshift(item)
    return item
  })
}

export function getNotices({ page = 1, size = 5, keyword = '', type = '' } = {}) {
  return remoteOrDemo(() => noticeApi.getNotices({ page, size, keyword, type }), () => pageOf(searchRecords(state.notices.filter((item) => !type || item.type === type), keyword, ['title', 'publisher']), page, size))
}

export function createNotice(data) {
  return remoteOrDemo(() => noticeApi.createNotice(data), () => {
    const item = { ...data, id: Date.now(), publisher: demoCurrentUser.name, publishAt: '刚刚', read: false, type: data.type || 'system' }
    state.notices.unshift(item)
    return item
  })
}

export function markNoticeRead(id) {
  return remoteOrDemo(() => noticeApi.markNoticeRead(id), () => {
    const item = state.notices.find((notice) => notice.id === id)
    if (item) item.read = true
    return item
  })
}

export function getFriends({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(() => friendApi.getFriends({ page, size, keyword }), () => pageOf(searchRecords(state.friends, keyword, ['name', 'account', 'department']), page, size))
}

export function addFriend(data) {
  return remoteOrDemo(() => friendApi.addFriend(data), () => ({ id: Date.now(), ...data, status: 'pending' }))
}

export function getGroups({ page = 1, size = 6 } = {}) {
  return remoteOrDemo(() => groupApi.getGroups({ page, size }), () => pageOf(state.groups, page, size))
}

export function createGroup(data) {
  return remoteOrDemo(() => groupApi.createGroup(data), () => {
    const item = { ...data, id: Date.now(), memberCount: 1, updatedAt: '刚刚' }
    state.groups.unshift(item)
    return item
  })
}

export function getSessions() {
  return remoteOrDemo(() => messageApi.getSessions(), () => state.sessions)
}

export function getMessages(sessionId) {
  return remoteOrDemo(() => messageApi.getMessages(messageTarget(sessionId)), () => state.messages[sessionId] || [])
}

export function sendMessage(sessionId, content) {
  return remoteOrDemo(() => messageApi.sendMessage({ ...messageTarget(sessionId), messageType: 'text', content }), () => {
    const message = { id: Date.now(), senderId: demoCurrentUser.id, senderName: demoCurrentUser.name, content, sentAt: '刚刚' }
    state.messages[sessionId] = [...(state.messages[sessionId] || []), message]
    const session = state.sessions.find((item) => item.id === sessionId)
    if (session) {
      session.lastMessage = content
      session.lastTime = '刚刚'
    }
    return message
  })
}

export function getDocuments({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(() => documentApi.getDocuments({ page, size, title: keyword }), () => pageOf(searchRecords(state.documents, keyword, ['title', 'summary', 'author']), page, size))
}

export function getDocument(id) {
  return remoteOrDemo(() => documentApi.getDocument(id), () => state.documents.find((document) => document.id === id))
}

export function saveDocument(data, id) {
  return remoteOrDemo(() => documentApi.saveDocument(data, id), () => {
    if (id) {
      const index = state.documents.findIndex((document) => document.id === id)
      state.documents[index] = { ...state.documents[index], ...data, updatedAt: '刚刚' }
      return state.documents[index]
    }
    const item = { ...data, id: Date.now(), summary: data.content?.slice(0, 40) || '暂无摘要', updatedAt: '刚刚', author: demoCurrentUser.name }
    state.documents.unshift(item)
    return item
  })
}

export function getSchedules({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(() => scheduleApi.getSchedules({ page, size, keyword }), () => pageOf(searchRecords(state.schedules, keyword, ['title', 'content', 'owner']), page, size))
}

export function createSchedule(data) {
  return remoteOrDemo(() => scheduleApi.createSchedule(data), () => {
    const item = { ...data, id: Date.now(), owner: demoCurrentUser.name }
    state.schedules.unshift(item)
    return item
  })
}
