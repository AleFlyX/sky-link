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
    // 登录页只需要判断本地是否存在凭据；真正的有效性仍由后端接口和 401 刷新流程确认。
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

router.beforeEach(async (to) => {
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
    // 未登录时保留原目标地址，登录成功后页面可以回到用户最初想访问的地址。
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const userStore = useUserStore()
  try {
    await userStore.ensureCurrentUserLoaded()
  } catch {
    if (!window.localStorage.getItem(TOKEN_KEY)) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  const requiredPermissions = Array.isArray(to.meta.permissions) ? to.meta.permissions : []
  if (!requiredPermissions.length) {
    return true
  }

  const permissions = new Set(userStore.user.permissions || [])
  const allowed = requiredPermissions.some((permission) => permissions.has(permission))
  if (!allowed) {
    // 路由数组采用“任一权限满足即可进入”的语义；这只是前端体验控制，不替代后端鉴权。
    return { name: 'unauthorized', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
