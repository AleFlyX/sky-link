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
  department: '',
  roleLabel: '',
  bio: '',
  lastLoginAt: '',
  roles: [],
  permissions: [],
}

function normalizeUser(user = {}) {
  const { avatar: _avatar, ...rest } = user

  return {
    ...emptyUser,
    ...rest,
    roles: Array.isArray(rest.roles) ? [...rest.roles] : [],
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

  const displayName = computed(() => user.value.name || user.value.account || '未登录')
  const avatarText = computed(() => displayName.value.slice(0, 1))

  function setUser(payload) {
    user.value = normalizeUser(payload)
    writeUserToStorage(user.value)
    isLoaded.value = true
  }

  function patchUser(payload = {}) {
    setUser({ ...user.value, ...payload })
  }

  function resetUser() {
    user.value = { ...emptyUser }
    clearUserStorage()
    isLoaded.value = false
  }

  async function loadCurrentUser() {
    const response = await fetchCurrentUser()
    setUser(unwrapUserResponse(response))
    isLoaded.value = true
    return user.value
  }

  return {
    user,
    isLoaded,
    displayName,
    avatarText,
    setUser,
    patchUser,
    resetUser,
    loadCurrentUser,
  }
})
