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
  // 保存当前 WebSocket 实例；它与 HTTP 请求实例不同，不能通过 Axios 拦截器自动管理。
  const socket = ref(null)
  // 断线重连只允许一个定时器，避免网络抖动产生多条并行连接。
  const reconnectTimer = ref(null)
  // 组件主动卸载/切换用户时设为 true，防止 onclose 又自动重连。
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
    // 连接时读取最新 access token；token 刷新后下次重连会自动使用新值。
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      return null
    }

    const explicitWebSocketUrl = import.meta.env.VITE_WS_URL // 允许通过环境变量显式指定 WebSocket URL，主要用于测试和调试。
    // 优先允许部署环境显式配置 WS 地址；否则从 API 基地址或当前站点推导。
    const apiBase =
      explicitWebSocketUrl || import.meta.env.VITE_API_BASE_URL || window.location.origin
    const url = new URL(apiBase, window.location.origin)
    // HTTPS 页面必须使用加密的 wss，HTTP 本地开发则使用 ws。
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    // 未显式配置时必须与后端 WebSocketConfiguration 的 /ws/messages 完全一致。
    url.pathname = explicitWebSocketUrl ? url.pathname : '/ws/messages'
    url.search = ''
    // WebSocket 握手无法复用 Axios 的 Authorization 拦截器，因此当前实现把 token 放到连接参数中。
    url.searchParams.set('token', token)
    return url.toString()
  }

  function scrollThreadToBottom(force = false) {
    nextTick(() => {
      const element = threadBodyRef.value
      if (!element) {
        return
      }
      // 如果用户滚动到接近底部的位置，或者强制滚动，则将滚动条滚动到底部。避免在用户查看历史消息时被新消息打断。
      const distanceToBottom = element.scrollHeight - element.scrollTop - element.clientHeight
      if (force || distanceToBottom < 120) {
        element.scrollTop = element.scrollHeight
      }
    })
  }
  // 同步当前会话的预览信息，包括最后一条消息和最后更新时间。
  function syncActiveSessionPreview() {
    if (!activeSession.value) {
      return
    }

    const latestMessage = messages.value.at(-1)
    if (!latestMessage) {
      return
    }
    // 更新当前会话的最后消息和最后时间戳，以便在会话列表中显示最新的预览信息。
    sessions.value = upsertSession(sessions.value, {
      ...activeSession.value,
      lastMessage: latestMessage,
      lastTime: latestMessage.sendTime,
    })
  }

  function closeSocket() {
    // 标记为主动关闭，使 onclose 区分“页面离开”与“网络异常”。
    closingSocket.value = true
    if (reconnectTimer.value) {
      window.clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
    if (socket.value) {
      // close 只发起关闭流程；真正的引用清理由 onclose 或此处置空完成。
      socket.value.close()
      socket.value = null
    }
  }

  function scheduleReconnect() {
    if (demoData.value || closingSocket.value) {
      // 演示数据无需真实连接；主动关闭后也绝不能再偷偷重连。
      return
    }
    if (reconnectTimer.value) {
      window.clearTimeout(reconnectTimer.value)
    }
    // 断线后延迟重连，并且始终只保留一个定时器，避免网络波动时同时创建多条连接。
    reconnectTimer.value = window.setTimeout(() => {
      reconnectTimer.value = null
      connectSocket()
    }, 3000)
  }

  function connectSocket() {
    if (demoData.value || !currentUserId.value) {
      // 未登录没有 Token 也没有可认证用户，不建立匿名实时消息连接。
      connectionState.value = demoData.value ? 'connected' : 'disconnected'
      return
    }

    if (
      socket.value &&
      (socket.value.readyState === WebSocket.OPEN ||
        socket.value.readyState === WebSocket.CONNECTING)
    ) {
      // 已连接或正在连接时不再创建第二个 WebSocket，防止同一消息被重复推送。
      return
    }

    const url = buildWebSocketUrl()
    if (!url) {
      connectionState.value = 'disconnected'
      return
    }

    closingSocket.value = false
    connectionState.value = 'connecting'
    // 此刻只建立推送通道；发送消息仍通过 HTTP API，以保留后端事务、好友和群成员校验。
    const ws = new WebSocket(url)
    socket.value = ws

    ws.onopen = () => {
      // 握手认证成功后才会触发 open；401/Origin 不通过通常会走 error/close。
      connectionState.value = 'connected'
    }

    ws.onmessage = (event) => {
      try {
        // 服务端 MessagePushService 推送的是 JSON：{ type, message, session }。
        const payload = JSON.parse(event.data)
        if (!payload?.type || !payload?.message) {
          // 忽略格式不完整的帧，避免异常数据污染页面消息列表。
          return
        }

        if (payload.session) {
          // 无论用户当前打开哪个会话，都先更新会话列表中的最后消息和时间。
          sessions.value = upsertSession(sessions.value, payload.session)
        }

        const conversationKey = getConversationKeyFromMessage(payload.message, currentUserId.value)
        if (conversationKey && conversationKey === activeSessionId.value) {
          // 只有当前正在看的会话才立即插入聊天窗口；其他会话只更新其列表预览。
          messages.value = upsertMessage(messages.value, payload.message)
          scrollThreadToBottom(payload.type === 'message.created')
        }
      } catch (error) {
        // 单个坏帧不能导致整条连接崩溃；保留警告方便开发者排查服务端协议问题。
        console.warn('ignored websocket message', error)
      }
    }

    ws.onerror = () => {
      // 浏览器不会提供可安全展示的底层错误细节；状态交给 onclose/reconnect 统一处理。
      connectionState.value = 'disconnected'
    }

    ws.onclose = () => {
      // 关闭后丢弃旧实例，下一次重连会创建全新的 WebSocket。
      socket.value = null
      if (!closingSocket.value) {
        // 非主动关闭视为断线，3 秒后重连；用户切换/组件卸载时不会走这里的重连逻辑。
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
      // 登录、退出或用户切换都先关旧连接再按新身份连接，避免一条 socket 混用两个人的 Token。
      closeSocket()
      connectSocket()
    },
    { immediate: true },
  )

  onMounted(async () => {
    await loadSessions()
  })

  onBeforeUnmount(() => {
    // 页面卸载时清理 socket 和重连定时器，避免离开消息页后仍保持后台推送连接。
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
