import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import { useUserStore } from '@/stores/user'
import { TOKEN_KEY } from '@/utils/request'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  if (['login', 'register', 'unauthorized'].includes(String(to.name))) {
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
