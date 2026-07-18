import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser as fetchCurrentUser } from '../api/user'

const STORAGE_KEY = 'skylink_user'

const emptyUser = {
  id: null,
  name: '',
  account: '',
  email: '',
  phone: '',
  departmentId: null,
  department: '',
  roleLabel: '',
  bio: '',
  lastLoginAt: '',
  roles: [],
  permissions: [],
}

const roleTextMap = {
  ROLE_SUPER_ADMIN: '超级管理员',
  ROLE_ADMIN: '管理员',
  ROLE_PROJECT_LEADER: '项目主管',
  ROLE_USER: '普通成员',
  'SUPER ADMINISTRATOR': '超级管理员',
  ADMINISTRATOR: '管理员',
  'PROJECT LEADER': '项目主管',
  USER: '普通成员',
  ADMIN: '管理员',
}

function normalizeRoleText(value) {
  const text = String(value || '').trim()
  if (!text) return ''

  return text
    .split(/[、,，;；|/]+/)
    .map((item) => {
      const token = item.trim()
      if (!token) return ''

      const mapped = roleTextMap[token.toUpperCase()]
      return mapped || token
    })
    .filter(Boolean)
    .join('、')
}

function getRoleText(role) {
  if (!role) return ''
  if (typeof role === 'string') return normalizeRoleText(role)
  return normalizeRoleText(
    role.roleCode || role.roleName || role.name || role.code || role.label || '',
  )
}

function normalizeRoles(roles) {
  if (!Array.isArray(roles)) return []
  return roles
    .map((role) => {
      if (!role || typeof role !== 'object') return role
      return {
        ...role,
        label: getRoleText(role),
      }
    })
    .filter(Boolean)
}

function normalizeUser(user = {}) {
  const { avatar: _avatar, ...rest } = user
  const roles = normalizeRoles(rest.roles)
  const roleLabel =
    normalizeRoleText(rest.roleLabel) || roles.map(getRoleText).filter(Boolean).join('、')

  return {
    ...emptyUser,
    ...rest,
    id: rest.id || rest.userId || null,
    name: rest.name || rest.nickname || rest.username || '',
    account: rest.account || rest.username || '',
    department: rest.department || rest.departmentName || '',
    roleLabel,
    roles,
    // 始终把权限整理成数组；路由守卫和 v-permission 都从这一个可信的前端状态读取。
    permissions: Array.isArray(rest.permissions) ? [...rest.permissions] : [],
  }
}

function readUserFromStorage() {
  if (typeof window === 'undefined') return { ...emptyUser }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? normalizeUser(JSON.parse(raw)) : { ...emptyUser }
  } catch {
    return { ...emptyUser }
  }
}

function writeUserToStorage(user) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

function clearUserStorage() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(STORAGE_KEY)
}

function unwrapUserResponse(response) {
  return response?.data?.userInfo || response?.data || response?.userInfo || response
}

export const useUserStore = defineStore('user', () => {
  const user = ref(readUserFromStorage())
  const isLoaded = ref(true)
  const currentUserLoaded = ref(false)
  let currentUserPromise = null

  const displayName = computed(() => user.value.name || user.value.account || '未登录')
  const avatarText = computed(() => displayName.value.slice(0, 1))

  function setUser(payload, options = {}) {
    // 先规范化再保存，避免不同接口字段名差异扩散到页面、路由和权限指令中。
    user.value = normalizeUser(payload)
    writeUserToStorage(user.value)
    isLoaded.value = true
    if (options.currentUserLoaded) {
      currentUserLoaded.value = true
    }
  }

  function patchUser(payload = {}) {
    setUser({ ...user.value, ...payload })
  }

  function resetUser() {
    user.value = { ...emptyUser }
    clearUserStorage()
    isLoaded.value = false
    currentUserLoaded.value = false
  }

  async function loadCurrentUser() {
    // 登录或刷新后重新向后端获取用户与权限，不能仅相信 localStorage 中的旧缓存。
    if (!currentUserPromise) {
      currentUserPromise = fetchCurrentUser()
        .then((response) => {
          setUser(unwrapUserResponse(response), { currentUserLoaded: true })
          isLoaded.value = true
          return user.value
        })
        .finally(() => {
          currentUserPromise = null
        })
    }

    return currentUserPromise
  }

  async function ensureCurrentUserLoaded() {
    if (currentUserLoaded.value) {
      return user.value
    }

    return loadCurrentUser()
  }

  return {
    user,
    isLoaded,
    currentUserLoaded,
    displayName,
    avatarText,
    setUser,
    patchUser,
    resetUser,
    loadCurrentUser,
    ensureCurrentUserLoaded,
  }
})
