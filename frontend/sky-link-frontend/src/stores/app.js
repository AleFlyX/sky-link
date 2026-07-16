import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { notifications } from '../mock/workspace'

const mockEnabled = import.meta.env.VITE_DATA_SOURCE === 'mock'

export const useAppStore = defineStore('app', () => {
  const appName = ref('SkyLink') // 应用名
  const initialized = ref(true) // 应用是否已初始化
  const notificationsState = ref(mockEnabled ? [...notifications] : []) // 通知列表

  const unreadNotificationCount = computed(
    // 未读通知数量
    () => notificationsState.value.filter((item) => !item.read).length,
  )

  /**
   * 标记所有通知为已读
   */
  function markNotificationsRead() {
    notificationsState.value = notificationsState.value.map((item) => ({
      ...item,
      read: true,
    }))
  }

  return {
    appName,
    initialized,
    notifications: notificationsState,
    unreadNotificationCount,
    markNotificationsRead,
  }
})
