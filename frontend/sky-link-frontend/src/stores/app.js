import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { currentUser, notifications } from '../mock/workspace'

export const useAppStore = defineStore('app', () => {
  const appName = ref('SkyLink')  // 应用名
  const initialized = ref(true) // 应用是否已初始化
  const currentUserState = ref(currentUser)// 当前用户信息
  const notificationsState = ref(notifications)// 通知列表

  const unreadNotificationCount = computed( // 未读通知数量
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
    currentUser: currentUserState,
    notifications: notificationsState,
    unreadNotificationCount,
    markNotificationsRead,
  }
})
