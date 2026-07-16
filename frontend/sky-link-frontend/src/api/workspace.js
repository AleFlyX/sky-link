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
import { buildSessionParams, getSessionIdFromParams, normalizeMessage } from '../utils/message'
import { departments, files, notices, tasks, users } from '../mock/workspace'

const demoCurrentUser = {
  id: 1001,
  name: '陈雨桐',
  account: 'chenyt',
  departmentId: 201,
  department: '产品研发中心',
  roleLabel: '产品管理员',
}

const demoFriends = [
  {
    id: 601,
    userId: 1002,
    name: '李明浩',
    account: 'limh',
    department: '综合管理部',
    status: '在线',
    lastSeen: '刚刚',
  },
  {
    id: 602,
    userId: 1003,
    name: '王嘉怡',
    account: 'wangjy',
    department: '产品研发中心',
    status: '忙碌',
    lastSeen: '10 分钟前',
  },
  {
    id: 603,
    userId: 1005,
    name: '赵思涵',
    account: 'zhaosh',
    department: '综合管理部',
    status: '离线',
    lastSeen: '昨天 18:40',
  },
]

function resolveDemoUser(userId) {
  if (userId === demoCurrentUser.id) {
    return {
      id: demoCurrentUser.id,
      name: demoCurrentUser.name,
      account: demoCurrentUser.account,
      department: demoCurrentUser.department,
      status: 'active',
    }
  }

  return users.find((item) => item.id === userId)
}

function createDemoGroupMember(userId, role, joinTime) {
  const user = resolveDemoUser(userId)

  return {
    userId,
    username: user?.account || `user${userId}`,
    nickname: user?.name || `用户#${userId}`,
    role,
    joinTime,
  }
}

const demoGroups = [
  {
    groupId: 701,
    groupName: 'Day 3 联调小组',
    notice: '今天完成消息、文档和日程演示收口。',
    ownerId: 1001,
    createTime: '2026-07-15T09:20:00',
    members: [
      createDemoGroupMember(1001, 'owner', '2026-07-15T09:20:00'),
      createDemoGroupMember(1002, 'admin', '2026-07-15T09:28:00'),
      createDemoGroupMember(1003, 'member', '2026-07-15T09:31:00'),
      createDemoGroupMember(1005, 'member', '2026-07-15T09:45:00'),
    ],
  },
  {
    groupId: 702,
    groupName: '产品研发中心',
    notice: '需求、设计与研发同步。',
    ownerId: 1003,
    createTime: '2026-07-14T17:30:00',
    members: [
      createDemoGroupMember(1003, 'owner', '2026-07-14T17:30:00'),
      createDemoGroupMember(1001, 'member', '2026-07-14T17:40:00'),
      createDemoGroupMember(1006, 'member', '2026-07-14T17:55:00'),
    ],
  },
]

const demoIncomingFriendRequests = [
  {
    requestId: 901,
    requestUser: {
      userId: 1004,
      username: 'zhouwy',
      nickname: '周婉仪',
      departmentName: '综合管理部',
      status: 1,
    },
    message: '你好，我想同步一下任务安排。',
    status: 'pending',
    requestTime: '2026-07-15T09:20:00',
  },
]

const demoOutgoingFriendRequests = [
  {
    requestId: 902,
    targetUser: {
      userId: 1006,
      username: 'liuxh',
      nickname: '刘雪涵',
      departmentName: '产品研发中心',
      status: 1,
    },
    message: '你好，一起联调消息模块吧。',
    status: 'pending',
    requestTime: '2026-07-15T10:05:00',
  },
  {
    requestId: 903,
    targetUser: {
      userId: 1007,
      username: 'hejy',
      nickname: '何嘉怡',
      departmentName: '综合管理部',
      status: 1,
    },
    message: '申请加你为好友，方便沟通资料。',
    status: 'accepted',
    requestTime: '2026-07-14T16:40:00',
  },
]

const demoSessions = [
  {
    id: 'single-1002',
    sessionType: 'single',
    targetId: 1002,
    targetName: '李明浩',
    lastTime: '10:32',
    lastMessage: '我已把部门名单同步好了。',
  },
  {
    id: 'group-701',
    sessionType: 'group',
    targetId: 701,
    targetName: 'Day 3 联调小组',
    lastTime: '09:48',
    lastMessage: '文档详情页可以开始验收了。',
  },
]

const demoMessages = {
  'single-1002': [
    {
      id: 801,
      senderId: 1002,
      senderName: '李明浩',
      content: '早上好，今天先联调消息和文档页面。',
      sentAt: '09:12',
    },
    {
      id: 802,
      senderId: 1001,
      senderName: '陈雨桐',
      content: '收到，我会把加载态和异常反馈一起补齐。',
      sentAt: '09:18',
    },
    {
      id: 803,
      senderId: 1002,
      senderName: '李明浩',
      content: '我已把部门名单同步好了。',
      sentAt: '10:32',
    },
  ],
  'group-701': [
    {
      id: 804,
      senderId: 1003,
      senderName: '王嘉怡',
      content: '任务筛选和分页已经可以演示。',
      sentAt: '09:40',
    },
    {
      id: 805,
      senderId: 1001,
      senderName: '陈雨桐',
      content: '文档详情页可以开始验收了。',
      sentAt: '09:48',
    },
  ],
}

const demoDocuments = [
  {
    id: 901,
    title: 'Day 3 全链路演示计划',
    summary: '从登录到管理页面的演示路径与验收记录。',
    status: 'team',
    updatedAt: '今天 10:20',
    author: '陈雨桐',
    content:
      '# Day 3 全链路演示计划\n\n完成登录、用户、文件、任务、公告，以及消息、文档、日程页面联调。',
  },
  {
    id: 902,
    title: '接口联调记录',
    summary: '记录后端接口状态、前端降级策略和待办事项。',
    status: 'private',
    updatedAt: '昨天 18:12',
    author: '李明浩',
    content:
      '# 接口联调记录\n\n当前后端接口尚未提供 Controller，前端通过统一数据服务保留可切换的演示数据。',
  },
]

const demoSchedules = [
  {
    id: 1001,
    title: '课程展示彩排',
    content: '走一遍登录、管理页和结果展示。',
    startTime: '2026-07-14 09:00',
    endTime: '2026-07-14 10:00',
    repeatType: 'none',
    owner: '陈雨桐',
  },
  {
    id: 1002,
    title: '后端接口联调',
    content: '确认用户、文件和任务接口返回结构。',
    startTime: '2026-07-14 14:00',
    endTime: '2026-07-14 15:30',
    repeatType: 'weekly',
    owner: '团队',
  },
]

function cloneDemoGroups() {
  return JSON.parse(JSON.stringify(demoGroups))
}

const state = {
  users: [...users],
  departments: [...departments],
  files: [...files],
  tasks: [...tasks],
  notices: [...notices],
  friends: [...demoFriends],
  incomingFriendRequests: [...demoIncomingFriendRequests],
  outgoingFriendRequests: [...demoOutgoingFriendRequests],
  groups: cloneDemoGroups(),
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
  return new Promise((resolve, reject) => {
    window.setTimeout(() => {
      try {
        resolve({ data: clone(factory()), source: 'demo', degraded: false })
      } catch (error) {
        reject(error)
      }
    }, 120)
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
  const value = String(keyword || '')
    .trim()
    .toLowerCase()
  if (!value) return records
  return records.filter((record) =>
    fields.some((field) =>
      String(record[field] ?? '')
        .toLowerCase()
        .includes(value),
    ),
  )
}

function sessionMessages(sessionId) {
  return [...(state.messages[sessionId] || [])]
}

function resolveMessagePayload(dataOrContent) {
  if (typeof dataOrContent === 'string') {
    return {
      messageType: 'text',
      content: dataOrContent,
    }
  }

  return {
    messageType: dataOrContent?.messageType || 'text',
    content: dataOrContent?.content || '',
  }
}

function applyDemoSessionPreview(sessionId, message) {
  const session = state.sessions.find((item) => item.id === sessionId)
  if (!session) {
    return
  }

  session.lastMessage = message.recalled ? '消息已撤回' : message.content
  session.lastTime = message.sendTime ?? message.sentAt ?? '刚刚'
}

function activeGroupMembers(group) {
  return (group?.members || []).filter((member) => member.role !== 'exited')
}

function findDemoGroup(groupId) {
  return state.groups.find((item) => Number(item.groupId) === Number(groupId)) || null
}

function getDemoGroupOrThrow(groupId) {
  const group = findDemoGroup(groupId)
  if (!group) {
    throw new Error('group not found')
  }
  return group
}

function getDemoMembership(groupId, userId) {
  return (
    activeGroupMembers(getDemoGroupOrThrow(groupId)).find((member) => member.userId === userId) ||
    null
  )
}

function requireDemoMembership(groupId, userId) {
  const membership = getDemoMembership(groupId, userId)
  if (!membership) {
    throw new Error('you are not a member of this group')
  }
  return membership
}

function requireDemoAdminOrOwner(groupId, userId) {
  const membership = requireDemoMembership(groupId, userId)
  if (!['owner', 'admin'].includes(membership.role)) {
    throw new Error('only group owner or admin can perform this operation')
  }
  return membership
}

function requireDemoOwner(groupId, userId) {
  const membership = requireDemoMembership(groupId, userId)
  if (membership.role !== 'owner') {
    throw new Error('only group owner can perform this operation')
  }
  return membership
}

function toDemoGroupSummary(group) {
  const owner = resolveDemoUser(group.ownerId)

  return {
    groupId: group.groupId,
    groupName: group.groupName,
    notice: group.notice,
    ownerId: group.ownerId,
    ownerName: owner?.name || owner?.account || '未知用户',
    memberCount: activeGroupMembers(group).length,
    createTime: group.createTime,
  }
}

function toDemoGroupDetail(group) {
  return {
    ...toDemoGroupSummary(group),
    members: activeGroupMembers(group),
  }
}

function toDemoGroupMember(groupId, userId) {
  const membership = getDemoMembership(groupId, userId)
  if (!membership) {
    return null
  }

  return {
    userId: membership.userId,
    username: membership.username,
    nickname: membership.nickname,
    role: membership.role,
    joinTime: membership.joinTime,
  }
}

function removeDemoGroupSession(groupId) {
  const sessionId = `group-${groupId}`
  state.sessions = state.sessions.filter((item) => item.id !== sessionId)
  delete state.messages[sessionId]
}

export function isDemoMode() {
  return mockEnabled
}

export async function login(account, password) {
  if (!mockEnabled) {
    const payload = await authApi.login(account, password)
    return payload?.data ?? payload
  }
  return demoResult(() => ({
    token: `demo-token-${Date.now()}`,
    expiresIn: 86400,
    userInfo: clone(demoCurrentUser),
  })).then((result) => result.data)
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
  return remoteOrDemo(
    () => userApi.getCurrentUser(),
    () => demoCurrentUser,
  )
}

export function getUsers({ page = 1, size = 5, keyword = '', status = '' } = {}) {
  return remoteOrDemo(
    () => userApi.getUsers({ page, size, keyword, status }),
    () =>
      pageOf(
        searchRecords(
          state.users.filter((item) => !status || item.status === status),
          keyword,
          ['name', 'account', 'department'],
        ),
        page,
        size,
      ),
  )
}

export function createUser(data) {
  return remoteOrDemo(
    () => userApi.createUser(data),
    () => {
      const item = {
        ...data,
        id: Date.now(),
        roles: ['普通成员'],
        status: data.status || 'active',
        updatedAt: '刚刚',
        phone: data.phone || '未填写',
      }
      state.users.unshift(item)
      return item
    },
  )
}

export function getDepartments({ page = 1, size = 5, keyword = '' } = {}) {
  refreshDemoDepartmentCounts()
  return remoteOrDemo(
    () => departmentApi.getDepartments({ page, size, keyword }),
    () =>
      pageOf(
        searchRecords(state.departments, keyword, ['name', 'leader', 'description']),
        page,
        size,
      ),
  )
}

export function createDepartment(data) {
  const payload = {
    departmentName: data?.departmentName || data?.name,
    leaderId: data?.leaderId || undefined,
    description: data?.description || undefined,
  }

  return remoteOrDemo(
    () => departmentApi.createDepartment(payload),
    () => {
      if (!String(payload.departmentName || '').trim()) {
        throw new Error('departmentName is required')
      }
      if (state.departments.some((item) => item.name === payload.departmentName)) {
        throw new Error('department name already exists')
      }

      const leader = state.users.find((item) => Number(item.id) === Number(payload.leaderId))
      const item = {
        id: Date.now(),
        name: payload.departmentName.trim(),
        leader: leader?.name || '',
        leaderId: payload.leaderId || null,
        memberCount: 0,
        description: payload.description || '',
      }
      state.departments.unshift(item)
      return item
    },
  )
}

export function updateDepartment(departmentId, data) {
  const payload = {
    departmentName: data?.departmentName || data?.name,
    leaderId: data?.leaderId || undefined,
    description: data?.description || undefined,
  }

  return remoteOrDemo(
    () => departmentApi.updateDepartment(departmentId, payload),
    () => {
      const item = state.departments.find(
        (department) => Number(department.id) === Number(departmentId),
      )
      if (!item) {
        throw new Error('department not found')
      }
      if (payload.departmentName != null) {
        const nextName = String(payload.departmentName).trim()
        if (!nextName) {
          throw new Error('departmentName cannot be blank')
        }
        if (
          state.departments.some(
            (department) => department.id !== item.id && department.name === nextName,
          )
        ) {
          throw new Error('department name already exists')
        }
        item.name = nextName
      }
      if (payload.leaderId !== undefined) {
        const leader = state.users.find((user) => Number(user.id) === Number(payload.leaderId))
        item.leaderId = payload.leaderId || null
        item.leader = leader?.name || ''
      }
      if (payload.description !== undefined) {
        item.description = payload.description || ''
      }
      return item
    },
  )
}

export function deleteDepartment(departmentId) {
  return remoteOrDemo(
    () => departmentApi.deleteDepartment(departmentId),
    () => {
      const item = state.departments.find(
        (department) => Number(department.id) === Number(departmentId),
      )
      if (!item) {
        throw new Error('department not found')
      }
      if (item.memberCount > 0) {
        throw new Error('department still has members')
      }
      state.departments = state.departments.filter(
        (department) => Number(department.id) !== Number(departmentId),
      )
      return null
    },
  )
}

export function getDepartmentMembers(departmentId, { page = 1, size = 10 } = {}) {
  return remoteOrDemo(
    () => departmentApi.getDepartmentMembers(departmentId, { page, size }),
    () => {
      const department = state.departments.find((item) => Number(item.id) === Number(departmentId))
      if (!department) {
        throw new Error('department not found')
      }
      return pageOf(
        state.users.filter((user) => user.department === department.name),
        page,
        size,
      )
    },
  )
}

export function addDepartmentMembers(departmentId, userIds) {
  return remoteOrDemo(
    () => departmentApi.addDepartmentMembers(departmentId, userIds),
    () => {
      const department = state.departments.find((item) => Number(item.id) === Number(departmentId))
      if (!department) {
        throw new Error('department not found')
      }

      const normalizedUserIds = [
        ...new Set((userIds || []).map((item) => Number(item)).filter(Boolean)),
      ]
      if (!normalizedUserIds.length) {
        throw new Error('userIds are required')
      }

      for (const userId of normalizedUserIds) {
        const user = state.users.find((item) => Number(item.id) === userId)
        if (!user) {
          throw new Error('some users do not exist')
        }
        user.department = department.name
      }

      refreshDemoDepartmentCounts()
      return pageOf(
        state.users.filter((user) => user.department === department.name),
        1,
        Math.max(20, normalizedUserIds.length),
      )
    },
  )
}

export function removeDepartmentMember(departmentId, userId) {
  return remoteOrDemo(
    () => departmentApi.removeDepartmentMember(departmentId, userId),
    () => {
      const department = state.departments.find((item) => Number(item.id) === Number(departmentId))
      if (!department) {
        throw new Error('department not found')
      }

      const user = state.users.find((item) => Number(item.id) === Number(userId))
      if (!user) {
        throw new Error('user not found')
      }
      if (user.department !== department.name) {
        throw new Error('user is not in this department')
      }

      user.department = '未分配部门'
      refreshDemoDepartmentCounts()
      return null
    },
  )
}

function refreshDemoDepartmentCounts() {
  for (const department of state.departments) {
    department.memberCount = state.users.filter(
      (user) => user.department === department.name,
    ).length
  }
}

export function getFiles({ page = 1, size = 5, keyword = '' } = {}) {
  return remoteOrDemo(
    () => fileApi.getFiles({ page, size, keyword }),
    () => pageOf(searchRecords(state.files, keyword, ['name', 'category', 'owner']), page, size),
  )
}

export function createFile(data) {
  return remoteOrDemo(
    () => fileApi.createFile(data),
    () => {
      const item = {
        ...data,
        id: Date.now(),
        owner: demoCurrentUser.name,
        size: '待上传',
        updatedAt: '刚刚',
      }
      state.files.unshift(item)
      return item
    },
  )
}

export function getTasks({ page = 1, size = 5, keyword = '', status = '' } = {}) {
  return remoteOrDemo(
    () => taskApi.getTasks({ page, size, keyword, status }),
    () =>
      pageOf(
        searchRecords(
          state.tasks.filter((item) => !status || item.status === status),
          keyword,
          ['title', 'assignee', 'priority'],
        ),
        page,
        size,
      ),
  )
}

export function createTask(data) {
  return remoteOrDemo(
    () => taskApi.createTask(data),
    () => {
      const executor = state.users.find((user) => Number(user.id) === Number(data.executorId))
      const item = {
        ...data,
        id: Date.now(),
        assignee: executor?.name || executor?.account || '未指定',
        priority: data.priority || 2,
        status: 'todo',
        dueDate: data.dueDate || data.deadline || '待定',
      }
      state.tasks.unshift(item)
      return item
    },
  )
}

export function updateTaskStatus(taskId, status) {
  return remoteOrDemo(
    () => taskApi.updateTaskStatus(taskId, status),
    () => {
      const item = state.tasks.find((task) => Number(task.id ?? task.taskId) === Number(taskId))
      if (!item) {
        throw new Error('task not found')
      }

      item.status = status
      return item
    },
  )
}

export function getNotices({ page = 1, size = 5, keyword = '', type = '' } = {}) {
  return remoteOrDemo(
    () => noticeApi.getNotices({ page, size, keyword, type }),
    () =>
      pageOf(
        searchRecords(
          state.notices.filter((item) => !type || item.type === type),
          keyword,
          ['title', 'publisher'],
        ),
        page,
        size,
      ),
  )
}

export function createNotice(data) {
  return remoteOrDemo(
    () => noticeApi.createNotice(data),
    () => {
      const item = {
        ...data,
        id: Date.now(),
        publisher: demoCurrentUser.name,
        publishAt: '刚刚',
        read: false,
        type: data.type || 'system',
      }
      state.notices.unshift(item)
      return item
    },
  )
}

export function markNoticeRead(id) {
  return remoteOrDemo(
    () => noticeApi.markNoticeRead(id),
    () => {
      const item = state.notices.find((notice) => notice.id === id)
      if (item) item.read = true
      return item
    },
  )
}

export function getFriends({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(
    () => friendApi.getFriends({ page, size, nickname: keyword }),
    () =>
      pageOf(searchRecords(state.friends, keyword, ['name', 'account', 'department']), page, size),
  )
}

export function addFriend(data) {
  return remoteOrDemo(
    () => friendApi.addFriend(data),
    () => {
      const targetUserId = Number(data?.friendUserId)
      const targetUser = state.users.find((item) => item.id === targetUserId)
      const request = {
        requestId: Date.now(),
        targetUser: {
          userId: targetUserId,
          username: targetUser?.account || `user${targetUserId}`,
          nickname: targetUser?.name || `用户#${targetUserId}`,
          departmentName: targetUser?.department || '未分配部门',
          status: targetUser?.status === 'disabled' ? 0 : 1,
        },
        message: data?.message || '',
        status: 'pending',
        requestTime: new Date().toISOString(),
      }
      state.outgoingFriendRequests.unshift(request)
      return { requestId: request.requestId, status: request.status }
    },
  )
}

export function getFriendRequests({ page = 1, size = 10 } = {}) {
  return remoteOrDemo(
    () => friendApi.getFriendRequests({ page, size }),
    () => pageOf(state.incomingFriendRequests, page, size),
  )
}

export function getSentFriendRequests({ page = 1, size = 10 } = {}) {
  return remoteOrDemo(
    () => friendApi.getSentFriendRequests({ page, size }),
    () => pageOf(state.outgoingFriendRequests, page, size),
  )
}

export function handleFriendRequest(requestId, action) {
  return remoteOrDemo(
    () => friendApi.handleFriendRequest(requestId, { action }),
    () => {
      const index = state.incomingFriendRequests.findIndex((item) => item.requestId === requestId)
      if (index < 0) {
        return null
      }

      const [request] = state.incomingFriendRequests.splice(index, 1)
      if (action !== 'accept') {
        return null
      }

      const user = request.requestUser
      const friend = {
        id: user.userId,
        userId: user.userId,
        name: user.nickname || user.username,
        account: user.username,
        department: user.departmentName || '未分配部门',
        status: '启用',
        lastSeen: '刚刚',
      }
      if (!state.friends.some((item) => item.userId === friend.userId)) {
        state.friends.unshift(friend)
      }

      return {
        friendUserId: user.userId,
        friendUser: user,
      }
    },
  )
}

export function getGroups({ page = 1, size = 6 } = {}) {
  return remoteOrDemo(
    () => groupApi.getGroups({ page, size }),
    () =>
      pageOf(
        state.groups
          .filter((group) => getDemoMembership(group.groupId, demoCurrentUser.id))
          .map(toDemoGroupSummary),
        page,
        size,
      ),
  )
}

export function createGroup(data) {
  const payload = {
    groupName: data?.groupName || data?.name,
    notice: data?.notice,
    memberIds: Array.isArray(data?.memberIds) ? data.memberIds : [],
  }

  return remoteOrDemo(
    () => groupApi.createGroup(payload),
    () => {
      const createdAt = new Date().toISOString()
      const nextGroupId = Date.now()
      const nextGroup = {
        groupId: nextGroupId,
        groupName: payload.groupName,
        notice: payload.notice || '',
        ownerId: demoCurrentUser.id,
        createTime: createdAt,
        members: [createDemoGroupMember(demoCurrentUser.id, 'owner', createdAt)],
      }

      for (const userId of payload.memberIds) {
        const user = resolveDemoUser(userId)
        if (!user) {
          throw new Error('some users do not exist')
        }
        if (
          userId !== demoCurrentUser.id &&
          !nextGroup.members.some((member) => member.userId === userId)
        ) {
          nextGroup.members.push(createDemoGroupMember(userId, 'member', createdAt))
        }
      }

      state.groups.unshift(nextGroup)
      return toDemoGroupDetail(nextGroup)
    },
  )
}

export function getGroup(groupId) {
  return remoteOrDemo(
    () => groupApi.getGroup(groupId),
    () => {
      requireDemoMembership(groupId, demoCurrentUser.id)
      return toDemoGroupDetail(getDemoGroupOrThrow(groupId))
    },
  )
}

export function updateGroup(groupId, data) {
  const payload = {
    groupName: data?.groupName,
    notice: data?.notice,
  }

  return remoteOrDemo(
    () => groupApi.updateGroup(groupId, payload),
    () => {
      requireDemoAdminOrOwner(groupId, demoCurrentUser.id)
      const group = getDemoGroupOrThrow(groupId)

      if (payload.groupName != null) {
        const value = String(payload.groupName).trim()
        if (!value) {
          throw new Error('groupName cannot be blank')
        }
        group.groupName = value
      }

      if (payload.notice != null) {
        group.notice = String(payload.notice).trim()
      }

      return toDemoGroupDetail(group)
    },
  )
}

export function getGroupMembers(groupId, { page = 1, size = 10 } = {}) {
  return remoteOrDemo(
    () => groupApi.getGroupMembers(groupId, { page, size }),
    () => {
      requireDemoMembership(groupId, demoCurrentUser.id)
      return pageOf(activeGroupMembers(getDemoGroupOrThrow(groupId)), page, size)
    },
  )
}

export function addGroupMembers(groupId, data) {
  const payload = {
    userIds: Array.isArray(data?.userIds) ? data.userIds : [],
  }

  return remoteOrDemo(
    () => groupApi.addGroupMembers(groupId, payload),
    () => {
      requireDemoAdminOrOwner(groupId, demoCurrentUser.id)
      if (!payload.userIds.length) {
        throw new Error('userIds are required')
      }

      const group = getDemoGroupOrThrow(groupId)
      const joinedAt = new Date().toISOString()
      const deduplicatedIds = [
        ...new Set(payload.userIds.map((item) => Number(item)).filter(Boolean)),
      ]
      const invitedMembers = []

      for (const userId of deduplicatedIds) {
        if (userId === demoCurrentUser.id) {
          continue
        }

        const user = resolveDemoUser(userId)
        if (!user) {
          throw new Error('some users do not exist')
        }

        const existing = group.members.find((member) => member.userId === userId)
        if (!existing) {
          const createdMember = createDemoGroupMember(userId, 'member', joinedAt)
          group.members.push(createdMember)
          invitedMembers.push(createdMember)
          continue
        }

        if (existing.role === 'exited') {
          existing.role = 'member'
          existing.joinTime = joinedAt
          invitedMembers.push(existing)
        }
      }

      return invitedMembers.map((member) => ({
        userId: member.userId,
        username: member.username,
        nickname: member.nickname,
        role: member.role,
        joinTime: member.joinTime,
      }))
    },
  )
}

export function removeGroupMember(groupId, userId) {
  return remoteOrDemo(
    () => groupApi.removeGroupMember(groupId, userId),
    () => {
      const operator = requireDemoAdminOrOwner(groupId, demoCurrentUser.id)
      const target = requireDemoMembership(groupId, Number(userId))

      if (target.userId === demoCurrentUser.id) {
        throw new Error('use leave endpoint to quit the group')
      }
      if (target.role === 'owner') {
        throw new Error('cannot remove the group owner')
      }
      if (operator.role === 'admin' && target.role !== 'member') {
        throw new Error('admin can only remove normal members')
      }

      target.role = 'exited'
      return null
    },
  )
}

export function updateGroupMemberRole(groupId, userId, role) {
  return remoteOrDemo(
    () => groupApi.updateGroupMemberRole(groupId, userId, role),
    () => {
      requireDemoOwner(groupId, demoCurrentUser.id)
      const membership = requireDemoMembership(groupId, Number(userId))

      if (membership.role === 'owner') {
        throw new Error('cannot modify owner role')
      }
      if (!['admin', 'member'].includes(role)) {
        throw new Error('role must be admin or member')
      }

      membership.role = role
      return toDemoGroupMember(groupId, Number(userId))
    },
  )
}

export function leaveGroup(groupId) {
  return remoteOrDemo(
    () => groupApi.leaveGroup(groupId),
    () => {
      const membership = requireDemoMembership(groupId, demoCurrentUser.id)
      if (membership.role === 'owner') {
        throw new Error('group owner cannot leave directly, dissolve the group instead')
      }

      membership.role = 'exited'
      removeDemoGroupSession(groupId)
      return null
    },
  )
}

export function deleteGroup(groupId) {
  return remoteOrDemo(
    () => groupApi.deleteGroup(groupId),
    () => {
      requireDemoOwner(groupId, demoCurrentUser.id)
      state.groups = state.groups.filter((group) => Number(group.groupId) !== Number(groupId))
      removeDemoGroupSession(groupId)
      return null
    },
  )
}

export function getSessions() {
  return remoteOrDemo(
    () => messageApi.getSessions(),
    () => state.sessions,
  )
}

export function getMessages(sessionOrParams, extraParams = {}) {
  const params = buildSessionParams(sessionOrParams, extraParams)
  const sessionId =
    typeof sessionOrParams === 'string' ? sessionOrParams : getSessionIdFromParams(params)

  return remoteOrDemo(
    () => messageApi.getMessages(params),
    () => {
      const records = sessionMessages(sessionId)
      const before =
        params.before == null
          ? records
          : records.filter((item) => (item.messageId ?? item.id) < Number(params.before))
      const size = Number(params.size) > 0 ? Number(params.size) : 20
      const paged = before.slice(Math.max(before.length - size, 0))

      return {
        total: before.length,
        page: 1,
        size,
        records: paged,
      }
    },
  )
}

export function sendMessage(sessionId, dataOrContent) {
  const payload = {
    ...buildSessionParams(sessionId),
    ...resolveMessagePayload(dataOrContent),
  }

  return remoteOrDemo(
    () => messageApi.sendMessage(payload),
    () => {
      const createdAt = new Date().toISOString()
      const message = normalizeMessage({
        id: Date.now(),
        senderId: demoCurrentUser.id,
        senderName: demoCurrentUser.name,
        ...payload,
        sendTime: createdAt,
      })

      state.messages[sessionId] = [...sessionMessages(sessionId), message]
      applyDemoSessionPreview(sessionId, message)
      return message
    },
  )
}

export function recallMessage(messageId, sessionId) {
  return remoteOrDemo(
    () => messageApi.recallMessage(messageId),
    () => {
      const records = sessionMessages(sessionId).map((item) => {
        if ((item.messageId ?? item.id) !== messageId) {
          return item
        }

        return normalizeMessage({
          ...item,
          recalled: true,
        })
      })

      state.messages[sessionId] = records
      const recalledMessage = records.find((item) => item.id === messageId)
      if (recalledMessage) {
        const latestMessage = records.at(-1)
        if (latestMessage) {
          applyDemoSessionPreview(sessionId, latestMessage)
        }
        return recalledMessage
      }

      return null
    },
  )
}

export function getDocuments({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(
    () => documentApi.getDocuments({ page, size, title: keyword }),
    () =>
      pageOf(searchRecords(state.documents, keyword, ['title', 'summary', 'author']), page, size),
  )
}

export function getDocument(id) {
  return remoteOrDemo(
    () => documentApi.getDocument(id),
    () => state.documents.find((document) => document.id === id),
  )
}

export function saveDocument(data, id) {
  return remoteOrDemo(
    () => documentApi.saveDocument(data, id),
    () => {
      if (id) {
        const index = state.documents.findIndex((document) => document.id === id)
        state.documents[index] = { ...state.documents[index], ...data, updatedAt: '刚刚' }
        return state.documents[index]
      }
      const item = {
        ...data,
        id: Date.now(),
        summary: data.content?.slice(0, 40) || '暂无摘要',
        updatedAt: '刚刚',
        author: demoCurrentUser.name,
      }
      state.documents.unshift(item)
      return item
    },
  )
}

export function getSchedules({ page = 1, size = 6, keyword = '' } = {}) {
  return remoteOrDemo(
    () => scheduleApi.getSchedules({ page, size, keyword }),
    () =>
      pageOf(searchRecords(state.schedules, keyword, ['title', 'content', 'owner']), page, size),
  )
}

export function createSchedule(data) {
  return remoteOrDemo(
    () => scheduleApi.createSchedule(data),
    () => {
      const item = { ...data, id: Date.now(), owner: demoCurrentUser.name }
      state.schedules.unshift(item)
      return item
    },
  )
}
