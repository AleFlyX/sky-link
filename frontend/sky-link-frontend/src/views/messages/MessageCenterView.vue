<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { TOKEN_KEY } from '../../utils/request'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppInput from '../../components/common/AppInput.vue'
import {
  getMessages,
  getSessions,
  isDemoMode,
  recallMessage as recallWorkspaceMessage,
  sendMessage as sendWorkspaceMessage,
} from '../../api/workspace'
import {
  buildRouteSession,
  canRecallMessage,
  getConversationKeyFromMessage,
  normalizeMessageList,
  normalizeSessionList,
  upsertMessage,
  upsertSession,
} from '../../utils/message'

const route = useRoute()
const userStore = useUserStore()
const PAGE_SIZE = 20

const sessions = ref([])
const activeSessionId = ref('')
const messages = ref([])
const draft = ref('')
const messageType = ref('text')
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

const currentUserId = computed(() => userStore.user.id ?? null)
const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value))
const connectionLabel = computed(() => {
  if (demoData.value) {
    return '演示模式'
  }
  return {
    connected: '实时在线',
    connecting: '连接中',
    disconnected: '连接已断开',
  }[connectionState.value] || '连接中'
})

function buildWebSocketUrl() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    return null
  }

  const explicitWebSocketUrl = import.meta.env.VITE_WS_URL
  const apiBase = explicitWebSocketUrl || import.meta.env.VITE_API_BASE_URL || window.location.origin
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

  if (socket.value && (
    socket.value.readyState === WebSocket.OPEN
    || socket.value.readyState === WebSocket.CONNECTING
  )) {
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
    const nextActiveSession = routeSession?.id
      || sessions.value.find((session) => session.id === activeSessionId.value)?.id
      || sessions.value[0]?.id
      || ''
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
      messageType: messageType.value,
      content,
    })
    const created = result.data || result

    draft.value = ''
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
    await ElMessageBox.confirm('确认撤回这条消息吗？', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning',
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

watch(activeSessionId, () => {
  loadMessages()
})

watch(
  () => [route.query.type, route.query.id, route.query.name],
  () => {
    loadSessions()
  },
)

watch(currentUserId, () => {
  closeSocket()
  connectSocket()
}, { immediate: true })

onMounted(async () => {
  await loadSessions()
})

onBeforeUnmount(() => {
  closeSocket()
})
</script>

<template>
  <div class="message-page">
    <AppCard title="消息中心" subtitle="查看会话列表与历史消息，支持发送文本和 emoji 消息">
      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        class="page-feedback"
      />
      <el-alert
        v-else-if="demoData"
        title="当前为演示模式，消息会即时更新会话"
        type="info"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <div class="message-layout">
        <aside class="message-sessions">
          <div class="message-sessions__heading">
            <strong>最近会话</strong>
            <span>{{ sessions.length }}</span>
          </div>
          <el-skeleton v-if="loading" :rows="5" animated />
          <button
            v-for="session in sessions"
            v-else
            :key="session.id"
            type="button"
            :class="['message-session', { 'message-session--active': session.id === activeSessionId }]"
            @click="activeSessionId = session.id"
          >
            <span class="message-session__avatar">{{ session.targetName?.slice(0, 1) || '?' }}</span>
            <span class="message-session__copy">
              <strong>{{ session.targetName }}</strong>
              <small>{{ session.lastMessage }}</small>
            </span>
          </button>
          <div v-if="!loading && !sessions.length" class="message-empty">暂无会话</div>
        </aside>

        <section class="message-thread">
          <div v-if="activeSession" class="message-thread__header">
            <div>
              <strong>{{ activeSession.targetName }}</strong>
              <span>{{ activeSession.sessionType === 'group' ? '群聊' : '单聊' }}</span>
            </div>
            <span class="message-thread__status">{{ connectionLabel }}</span>
          </div>
          <div v-else class="message-thread__placeholder">
            从通讯录选择一个好友或群聊后，就可以在这里开始对话。
          </div>

          <el-skeleton v-if="messageLoading" :rows="6" animated />
          <div v-else-if="activeSession && !messages.length" class="message-empty">暂无历史消息，先发一条吧</div>
          <div
            v-else
            ref="threadBodyRef"
            class="message-thread__body"
          >
            <div class="message-thread__history">
              <AppButton
                v-if="hasMoreHistory"
                size="small"
                variant="secondary"
                :loading="loadingHistory"
                @click="handleLoadMoreHistory"
              >
                加载更早消息
              </AppButton>
            </div>
            <article
              v-for="message in messages"
              :key="message.id"
              :class="['message-bubble', { 'message-bubble--mine': message.senderId === currentUserId }]"
            >
              <div class="message-bubble__meta">
                <span class="message-bubble__sender">{{ message.senderName }} · {{ message.sentAt }}</span>
                <button
                  v-if="canRecall(message)"
                  type="button"
                  class="message-bubble__action"
                  @click="handleRecall(message)"
                >
                  撤回
                </button>
              </div>
              <p>{{ message.recalled ? '消息已撤回' : message.content }}</p>
            </article>
          </div>

          <div class="message-composer">
            <div class="message-composer__mode">
              <el-radio-group v-model="messageType" size="small">
                <el-radio-button label="text">文本</el-radio-button>
                <el-radio-button label="emoji">Emoji</el-radio-button>
              </el-radio-group>
            </div>
            <AppInput
              v-model="draft"
              type="textarea"
              :rows="2"
              resize="none"
              placeholder="输入消息，Ctrl+Enter 发送"
              @keyup.ctrl.enter="handleSend"
            />
            <AppButton variant="primary" :disabled="!draft.trim() || !activeSession" @click="handleSend">
              发送
            </AppButton>
          </div>
        </section>
      </div>
    </AppCard>
  </div>
</template>

<style scoped>
.message-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.34fr) minmax(0, 1fr);
  min-height: 32rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.message-sessions {
  padding: 1rem;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface-muted);
}

.message-sessions__heading,
.message-thread__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.message-sessions__heading {
  margin-bottom: 0.75rem;
  color: var(--color-text);
}

.message-sessions__heading span,
.message-thread__header span,
.message-thread__status {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.message-session {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.7rem;
  margin-top: 0.35rem;
  padding: 0.75rem;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.message-session:hover,
.message-session--active {
  border-color: rgba(51, 112, 255, 0.18);
  background: var(--color-surface);
}

.message-session__avatar {
  display: grid;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;
  border-radius: 0.75rem;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 700;
}

.message-session__copy {
  min-width: 0;
  display: grid;
  gap: 0.2rem;
  flex: 1;
}

.message-session__copy strong,
.message-session__copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-session__copy small {
  color: var(--color-text-muted);
}

.message-thread {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.message-thread__placeholder {
  display: grid;
  place-items: center;
  min-height: 8rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.message-thread__header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.message-thread__header > div {
  display: grid;
  gap: 0.2rem;
}

.message-thread__body {
  flex: 1;
  display: grid;
  align-content: start;
  gap: 0.8rem;
  padding: 1.25rem;
  overflow-y: auto;
}

.message-thread__history {
  display: flex;
  justify-content: center;
}

.message-bubble {
  width: fit-content;
  max-width: min(80%, 32rem);
  padding: 0.7rem 0.85rem;
  border-radius: 0.8rem 0.8rem 0.8rem 0.2rem;
  background: var(--color-surface-muted);
}

.message-bubble--mine {
  justify-self: end;
  border-radius: 0.8rem 0.8rem 0.2rem 0.8rem;
  background: var(--color-primary-soft);
}

.message-bubble__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.message-bubble__sender {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.message-bubble__action {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font-size: 0.75rem;
  cursor: pointer;
}

.message-bubble p {
  margin: 0.25rem 0 0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-composer {
  display: flex;
  align-items: end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}

.message-composer__mode {
  align-self: stretch;
  display: flex;
  align-items: end;
}

.message-composer .el-textarea {
  flex: 1;
}

.message-empty {
  display: grid;
  place-items: center;
  min-height: 8rem;
  color: var(--color-text-muted);
}

@media (max-width: 760px) {
  .message-layout {
    grid-template-columns: 1fr;
  }

  .message-sessions {
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .message-session {
    display: inline-flex;
    width: calc(50% - 0.25rem);
    vertical-align: top;
  }

  .message-composer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
