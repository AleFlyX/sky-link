import { watch } from 'vue'
import { useUserStore } from '../stores/user'

const permissionState = Symbol('permissionState')

function normalizePermissions(value) {
  if (typeof value === 'string') {
    return value ? [value] : []
  }

  if (Array.isArray(value)) {
    return value.filter((permission) => typeof permission === 'string' && permission)
  }

  return []
}

function getPermissionOptions(value) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return {
      permissions: normalizePermissions(value.permissions ?? value.permission ?? value.value),
      mode: value.mode === 'all' ? 'all' : 'any',
    }
  }

  return {
    permissions: normalizePermissions(value),
    mode: 'any',
  }
}

export function hasPermission(userPermissions, value) {
  const { permissions, mode } = getPermissionOptions(value)
  const grantedPermissions = new Set(normalizePermissions(userPermissions))

  if (!permissions.length) {
    return false
  }

  return mode === 'all'
    // all 适用于一个按钮同时需要多个能力的情况。
    ? permissions.every((permission) => grantedPermissions.has(permission))
    // 默认 any：拥有声明数组中的任意一个权限即可展示。
    : permissions.some((permission) => grantedPermissions.has(permission))
}

function updateVisibility(el, value) {
  const state = el[permissionState]
  const allowed = hasPermission(state.userStore.user.permissions, value)

  // 只改变可见性，不能当作安全边界；绕过页面后，后端仍必须拒绝无权限请求。
  el.style.display = allowed ? state.originalDisplay : 'none'
}

function bindPermission(el, binding) {
  const state = el[permissionState]
  state.value = binding.value ?? binding.arg
  updateVisibility(el, state.value)
}

export const permission = {
  mounted(el, binding) {
    const userStore = useUserStore()
    el[permissionState] = {
      originalDisplay: el.style.display,
      userStore,
      value: binding.value ?? binding.arg,
      stop: watch(
        // 用户资料刷新导致 permissions 变化时，按钮会重新判断是否显示。
        () => userStore.user.permissions,
        () => updateVisibility(el, el[permissionState].value),
        { deep: true },
      ),
    }

    bindPermission(el, binding)
  },

  updated(el, binding) {
    bindPermission(el, binding)
  },

  beforeUnmount(el) {
    el[permissionState]?.stop()
    delete el[permissionState]
  },
}

export default permission
