<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '../../stores/app'
import { TOKEN_KEY } from '../../utils/request'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import { getMessages, getSessions, isDemoMode } from '../../api/workspace'
import { recallMessage as apiRecallMessage, sendMessage as apiSendMessage } from '../../api/message'

const appStore = useAppStore()
const route = useRoute()
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

const currentUserId = computed(() => appStore.currentUser?.id ?? null)
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

function toDate(value) {
  if (value instanceof Date) {
    return value
  }
  if (value === null || value === undefined || value === '') {
    return null
  }

  const raw = String(value)
  const directDate = new Date(raw)
  if (!Number.isNaN(directDate.getTime())) {
    return directDate
  }

  const timeOnlyMatch = raw.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/)
  if (!timeOnlyMatch) {
    return null
  }

  const [, hourText, minuteText, secondText = '0'] = timeOnlyMatch
  const date = new Date()
  date.setHours(Number(hourText), Number(minuteText), Number(secondText), 0)
  return date
}

function formatMessageTime(value) {
  const date = toDate(value)
  if (!date) {
    return value ? String(value) : ''
  }

  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function normalizeMessage(message) {
  const sendTime = message?.sendTime ?? message?.sentAt ?? message?.sentAtLabel ?? null
  const recalled = Boolean(message?.recalled ?? message?.isRecalled)
  const resolvedSendTime = sendTime === '刚刚' ? new Date().toISOString() : sendTime
  return {
    id: message?.messageId ?? message?.id,
    senderId: message?.senderId,
    senderName: message?.senderName,
    receiverId: message?.receiverId,
    groupId: message?.groupId,
    messageType: message?.messageType ?? 'text',
    content: recalled ? '消息已撤回' : message?.content,
    recalled,
    sendTime: resolvedSendTime,
    sentAt: formatMessageTime(resolvedSendTime),
  }
}

function normalizeSession(session) {
  const lastMessage = session?.lastMessage
    ? normalizeMessage(session.lastMessage)
    : null
  return {
    id: session?.id ?? `${session?.sessionType}-${session?.targetId}`,
    sessionType: session?.sessionType,
    targetId: session?.targetId,
    targetName: session?.targetName,
    lastMessage: lastMessage?.content || session?.lastMessage || '',
    lastTime: session?.lastTime || lastMessage?.sentAt || '',
  }
}

function normalizeSessionList(data) {
  const list = Array.isArray(data) ? data : data?.records ?? []
  return list.map(normalizeSession)
}

function normalizeMessageList(data) {
  const list = Array.isArray(data) ? data : data?.records ?? []
  return list.map(normalizeMessage)
}

function buildRouteSession() {
  const sessionType = String(route.query.type || '').trim()
  const targetId = Number(route.query.id)
  if (!['single', 'group'].includes(sessionType) || !Number.isFinite(targetId) || targetId <= 0) {
    return null
  }

  const targetName = typeof route.query.name === 'string' && route.query.name.trim()
    ? route.query.name.trim()
    : sessionType === 'group'
      ? `群聊#${targetId}`
      : `用户#${targetId}`

  return {
    id: `${sessionType}-${targetId}`,
    sessionType,
    targetId,
    targetName,
    lastMessage: '',
    lastTime: '',
  }
}

function getConversationKeyFromMessage(message) {
  if (!message) {
    return ''
  }
  if (message.groupId) {
    return `group-${message.groupId}`
  }
  const otherUserId = message.senderId === currentUserId.value ? message.receiverId : message.senderId
  return otherUserId ? `single-${otherUserId}` : ''
}

function buildWebSocketUrl() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    return null
  }

  const apiBase = import.meta.env.VITE_API_BASE_URL || window.location.origin
  const url = new URL(apiBase, window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws/messages'
  url.search = ''
  url.searchParams.set('token', token)
  return url.toString()
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

      const conversationKey = getConversationKeyFromMessage(payload.message)
      if (conversationKey && conversationKey === activeSessionId.value) {
        loadMessages()
      }
      loadSessions()
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
    const routeSession = buildRouteSession()
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
    }
  } catch (error) {
    loadError.value = error.message || '会话加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  if (!activeSessionId.value) {
    messages.value = []
    return
  }

  messageLoading.value = true
  loadError.value = ''
  try {
    const result = await getMessages(activeSessionId.value)
    const payload = Array.isArray(result.data) ? result.data : result.data?.records ?? []
    messages.value = normalizeMessageList(payload)
    demoData.value = result.source === 'demo' || demoData.value
  } catch (error) {
    loadError.value = error.message || '消息加载失败'
  } finally {
    messageLoading.value = false
  }
}

function refreshCurrentView() {
  loadMessages()
  loadSessions()
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || !activeSessionId.value) {
    return
  }

  try {
    if (demoData.value) {
      const createdAt = new Date().toISOString()
      const result = {
        data: normalizeMessage({
          id: Date.now(),
          senderId: currentUserId.value,
          senderName: appStore.currentUser?.name || '',
          content,
          messageType: messageType.value,
          sentAt: createdAt,
          sendTime: createdAt,
        }),
      }
      draft.value = ''
      const created = normalizeMessage(result.data || result)
      messages.value = [...messages.value, created]
      const session = sessions.value.find((item) => item.id === activeSessionId.value)
      if (session) {
        session.lastMessage = created.content
        session.lastTime = created.sentAt
      }
    } else {
      const activeTarget = activeSession.value
      if (!activeTarget) {
        return
      }
      const payload = activeTarget.sessionType === 'group'
        ? { groupId: activeTarget.targetId, messageType: messageType.value, content }
        : { receiverId: activeTarget.targetId, messageType: messageType.value, content }
      await apiSendMessage(payload)
      draft.value = ''
      await refreshCurrentView()
    }
    ElMessage.success('消息已发送')
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}

function applyRecallLocally(messageId) {
  const currentMessage = messages.value.find((message) => message.id === messageId)
  const wasLastMessage = sessions.value.find((item) => item.id === activeSessionId.value)?.lastMessage === currentMessage?.content

  messages.value = messages.value.map((message) => (
    message.id === messageId
      ? {
          ...message,
          recalled: true,
          content: '消息已撤回',
        }
      : message
  ))

  const recalledMessage = messages.value.find((message) => message.id === messageId)
  if (!recalledMessage) {
    return
  }

  const session = sessions.value.find((item) => item.id === activeSessionId.value)
  if (session && wasLastMessage) {
    session.lastMessage = recalledMessage.content
    session.lastTime = recalledMessage.sentAt
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
    if (demoData.value) {
      applyRecallLocally(message.id)
    } else {
      await apiRecallMessage(message.id)
      await refreshCurrentView()
    }
    ElMessage.success('消息已撤回')
  } catch (error) {
    ElMessage.error(error.message || '撤回失败')
  }
}

function canRecall(message) {
  if (!message || message.senderId !== currentUserId.value || message.recalled) {
    return false
  }
  const sendTime = toDate(message.sendTime ?? message.sentAt ?? message.sentAtLabel)
  if (!sendTime) {
    return true
  }
  return Date.now() - sendTime.getTime() <= 120000
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

onMounted(async () => {
  await loadSessions()
  connectSocket()
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

          <el-skeleton v-if="messageLoading" :rows="6" animated />
          <div v-else-if="!messages.length" class="message-empty">暂无历史消息，先发一条吧</div>
          <div v-else class="message-thread__body">
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
            <el-input
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
