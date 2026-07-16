import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import { useUserStore } from '@/stores/user'
import { TOKEN_KEY } from '@/utils/request'

const USER_STORAGE_KEY = 'skylink_user'
const AUTH_ROUTE_NAMES = new Set(['login', 'register'])

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

function hasPersistedAuthState() {
  if (typeof window === 'undefined') return false

  const token = window.localStorage.getItem(TOKEN_KEY)
  if (token) {
    return true
  }

  const rawUser = window.localStorage.getItem(USER_STORAGE_KEY)
  if (!rawUser) {
    return false
  }

  try {
    return JSON.parse(rawUser) != null
  } catch {
    return false
  }
}

router.beforeEach((to) => {
  if (AUTH_ROUTE_NAMES.has(String(to.name))) {
    if (hasPersistedAuthState()) {
      return { name: 'dashboard' }
    }

    return true
  }

  if (String(to.name) === 'unauthorized') {
    return true
  }

  if (!String(to.path).startsWith('/app')) {
    return true
  }

  const token = window.localStorage.getItem(TOKEN_KEY)
  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const requiredPermissions = Array.isArray(to.meta.permissions) ? to.meta.permissions : []
  if (!requiredPermissions.length) {
    return true
  }

  const userStore = useUserStore()
  const permissions = new Set(userStore.user.permissions || [])
  const allowed = requiredPermissions.some((permission) => permissions.has(permission))
  if (!allowed) {
    return { name: 'unauthorized', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
