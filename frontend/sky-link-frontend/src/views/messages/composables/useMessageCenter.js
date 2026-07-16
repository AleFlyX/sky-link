import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../../stores/user'
import { useConfirmDialog } from '../../../composables/useConfirmDialog'
import { TOKEN_KEY } from '../../../utils/request'
import {
  getMessages,
  getSessions,
  isDemoMode,
  recallMessage as recallWorkspaceMessage,
  sendMessage as sendWorkspaceMessage,
} from '../../../api/workspace'
import {
  buildRouteSession,
  canRecallMessage,
  getConversationKeyFromMessage,
  normalizeMessageList,
  normalizeSessionList,
  upsertMessage,
  upsertSession,
} from '../../../utils/message'

const PAGE_SIZE = 20
const emojiOptions = ['😀', '😄', '😂', '😊', '😍', '🥳', '👍', '🙏', '🎉', '🔥', '💡', '❤️']

export function useMessageCenter() {
  const route = useRoute()
  const userStore = useUserStore()

  const sessions = ref([])
  const activeSessionId = ref('')
  const messages = ref([])
  const draft = ref('')
  const emojiPopoverVisible = ref(false)
  const loading = ref(false)
  const messageLoading = ref(false)
  const loadError = ref('')
  const demoData = ref(isDemoMode())
  const connectionState = ref('connecting')
  const socket = ref(null)
  const reconnectTimer = ref(null)
  const closingSocket = ref(false)
  const hasMoreHistory = ref(false)
  const loadingHistory = ref(false)
  const threadBodyRef = ref(null)
  const { confirm } = useConfirmDialog()

  const currentUserId = computed(() => userStore.user.id ?? null)
  const activeSession = computed(() =>
    sessions.value.find((session) => session.id === activeSessionId.value),
  )
  const connectionLabel = computed(() => {
    if (demoData.value) {
      return '演示模式'
    }
    return (
      {
        connected: '实时在线',
        connecting: '连接中',
        disconnected: '连接已断开',
      }[connectionState.value] || '连接中'
    )
  })

  function buildWebSocketUrl() {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      return null
    }

    const explicitWebSocketUrl = import.meta.env.VITE_WS_URL
    const apiBase =
      explicitWebSocketUrl || import.meta.env.VITE_API_BASE_URL || window.location.origin
    const url = new URL(apiBase, window.location.origin)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = explicitWebSocketUrl ? url.pathname : '/ws/messages'
    url.search = ''
    url.searchParams.set('token', token)
    return url.toString()
  }

  function scrollThreadToBottom(force = false) {
    nextTick(() => {
      const element = threadBodyRef.value
      if (!element) {
        return
      }

      const distanceToBottom = element.scrollHeight - element.scrollTop - element.clientHeight
      if (force || distanceToBottom < 120) {
        element.scrollTop = element.scrollHeight
      }
    })
  }

  function syncActiveSessionPreview() {
    if (!activeSession.value) {
      return
    }

    const latestMessage = messages.value.at(-1)
    if (!latestMessage) {
      return
    }

    sessions.value = upsertSession(sessions.value, {
      ...activeSession.value,
      lastMessage: latestMessage,
      lastTime: latestMessage.sendTime,
    })
  }

  function closeSocket() {
    closingSocket.value = true
    if (reconnectTimer.value) {
      window.clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
    if (socket.value) {
      socket.value.close()
      socket.value = null
    }
  }

  function scheduleReconnect() {
    if (demoData.value || closingSocket.value) {
      return
    }
    if (reconnectTimer.value) {
      window.clearTimeout(reconnectTimer.value)
    }
    reconnectTimer.value = window.setTimeout(() => {
      reconnectTimer.value = null
      connectSocket()
    }, 3000)
  }

  function connectSocket() {
    if (demoData.value || !currentUserId.value) {
      connectionState.value = demoData.value ? 'connected' : 'disconnected'
      return
    }

    if (
      socket.value &&
      (socket.value.readyState === WebSocket.OPEN ||
        socket.value.readyState === WebSocket.CONNECTING)
    ) {
      return
    }

    const url = buildWebSocketUrl()
    if (!url) {
      connectionState.value = 'disconnected'
      return
    }

    closingSocket.value = false
    connectionState.value = 'connecting'
    const ws = new WebSocket(url)
    socket.value = ws

    ws.onopen = () => {
      connectionState.value = 'connected'
    }

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)
        if (!payload?.type || !payload?.message) {
          return
        }

        if (payload.session) {
          sessions.value = upsertSession(sessions.value, payload.session)
        }

        const conversationKey = getConversationKeyFromMessage(payload.message, currentUserId.value)
        if (conversationKey && conversationKey === activeSessionId.value) {
          messages.value = upsertMessage(messages.value, payload.message)
          scrollThreadToBottom(payload.type === 'message.created')
        }
      } catch (error) {
        console.warn('ignored websocket message', error)
      }
    }

    ws.onerror = () => {
      connectionState.value = 'disconnected'
    }

    ws.onclose = () => {
      socket.value = null
      if (!closingSocket.value) {
        connectionState.value = 'disconnected'
        scheduleReconnect()
      }
    }
  }

  async function loadSessions() {
    loading.value = true
    loadError.value = ''
    try {
      const result = await getSessions()
      const normalized = normalizeSessionList(result.data)
      const routeSession = buildRouteSession(route.query)
      if (routeSession && !normalized.some((session) => session.id === routeSession.id)) {
        normalized.unshift(routeSession)
      }
      sessions.value = normalized
      demoData.value = result.source === 'demo' || demoData.value
      const nextActiveSession =
        routeSession?.id ||
        sessions.value.find((session) => session.id === activeSessionId.value)?.id ||
        sessions.value[0]?.id ||
        ''
      activeSessionId.value = nextActiveSession
      if (!nextActiveSession) {
        messages.value = []
        hasMoreHistory.value = false
      }
    } catch (error) {
      loadError.value = error.message || '会话加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadMessages({ before = null } = {}) {
    if (!activeSessionId.value) {
      messages.value = []
      hasMoreHistory.value = false
      return
    }

    if (before) {
      loadingHistory.value = true
    } else {
      messageLoading.value = true
    }

    const scrollElement = threadBodyRef.value
    const previousHeight = scrollElement?.scrollHeight || 0
    loadError.value = ''
    try {
      const result = await getMessages(activeSessionId.value, {
        before,
        size: PAGE_SIZE,
      })
      const payload = result.data?.records ? result.data : { records: result.data || [] }
      const incomingMessages = normalizeMessageList(payload)

      if (before) {
        let mergedMessages = [...messages.value]
        incomingMessages.forEach((message) => {
          mergedMessages = upsertMessage(mergedMessages, message)
        })
        messages.value = mergedMessages
      } else {
        messages.value = incomingMessages
      }

      hasMoreHistory.value = Number(payload.total || 0) > incomingMessages.length
      demoData.value = result.source === 'demo' || demoData.value

      await nextTick()
      if (before && scrollElement) {
        const nextHeight = scrollElement.scrollHeight
        scrollElement.scrollTop = nextHeight - previousHeight
      } else {
        scrollThreadToBottom(true)
      }
    } catch (error) {
      loadError.value = error.message || '消息加载失败'
    } finally {
      if (before) {
        loadingHistory.value = false
      } else {
        messageLoading.value = false
      }
    }
  }

  function handleLoadMoreHistory() {
    const oldestMessageId = messages.value[0]?.id
    if (!oldestMessageId || loadingHistory.value) {
      return
    }
    loadMessages({ before: oldestMessageId })
  }

  async function handleSend() {
    const content = draft.value.trim()
    if (!content || !activeSessionId.value) {
      return
    }

    try {
      const result = await sendWorkspaceMessage(activeSessionId.value, {
        messageType: 'text',
        content,
      })
      const created = result.data || result

      draft.value = ''
      if (created) {
        messages.value = upsertMessage(messages.value, created)
        syncActiveSessionPreview()
        scrollThreadToBottom(true)
      }
      emojiPopoverVisible.value = false

      ElMessage.success('消息已发送')
    } catch (error) {
      ElMessage.error(error.message || '发送失败')
    }
  }

  async function handleSendEmoji(emoji) {
    emojiPopoverVisible.value = false
    if (!emoji || !activeSessionId.value) {
      return
    }

    try {
      const result = await sendWorkspaceMessage(activeSessionId.value, {
        messageType: 'emoji',
        content: emoji,
      })
      const created = result.data || result

      if (created) {
        messages.value = upsertMessage(messages.value, created)
        syncActiveSessionPreview()
        scrollThreadToBottom(true)
      }

      ElMessage.success('消息已发送')
    } catch (error) {
      ElMessage.error(error.message || '发送失败')
    }
  }

  async function handleRecall(message) {
    try {
      await confirm('确认撤回这条消息吗？', '撤回消息', {
        confirmText: '撤回',
        cancelText: '取消',
        type: 'danger',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    try {
      const result = await recallWorkspaceMessage(message.id, activeSessionId.value)
      const recalledMessage = result.data || result

      if (recalledMessage) {
        messages.value = upsertMessage(messages.value, recalledMessage)
        syncActiveSessionPreview()
      }
      ElMessage.success('消息已撤回')
    } catch (error) {
      ElMessage.error(error.message || '撤回失败')
    }
  }

  function canRecall(message) {
    return canRecallMessage(message, currentUserId.value)
  }

  function selectSession(sessionId) {
    emojiPopoverVisible.value = false
    activeSessionId.value = sessionId
  }

  watch(activeSessionId, () => {
    emojiPopoverVisible.value = false
    loadMessages()
  })

  watch(
    () => [route.query.type, route.query.id, route.query.name],
    () => {
      loadSessions()
    },
  )

  watch(
    currentUserId,
    () => {
      closeSocket()
      connectSocket()
    },
    { immediate: true },
  )

  onMounted(async () => {
    await loadSessions()
  })

  onBeforeUnmount(() => {
    closeSocket()
  })

  return {
    activeSession,
    activeSessionId,
    canRecall,
    connectionLabel,
    currentUserId,
    demoData,
    draft,
    emojiOptions,
    emojiPopoverVisible,
    handleLoadMoreHistory,
    handleRecall,
    handleSend,
    handleSendEmoji,
    hasMoreHistory,
    loadError,
    loading,
    loadingHistory,
    messageLoading,
    messages,
    selectSession,
    sessions,
    threadBodyRef,
  }
}
