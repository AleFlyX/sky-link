<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import { getMessages, getSessions, isDemoMode, sendMessage } from '../../api/workspace'

const sessions = ref([])
const activeSessionId = ref('')
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const messageLoading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())

const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value))

async function loadSessions() {
  loading.value = true
  loadError.value = ''
  const result = await getSessions()
  sessions.value = result.data || []
  demoData.value = result.source === 'demo'
  if (result.degraded) loadError.value = `会话接口暂不可用，已切换演示数据：${result.error}`
  if (!activeSessionId.value && sessions.value.length) activeSessionId.value = sessions.value[0].id
  loading.value = false
}

async function loadMessages() {
  if (!activeSessionId.value) {
    messages.value = []
    return
  }
  messageLoading.value = true
  const result = await getMessages(activeSessionId.value)
  messages.value = result.data || []
  demoData.value = result.source === 'demo' || demoData.value
  if (result.degraded) loadError.value = `历史消息接口暂不可用，已切换演示数据：${result.error}`
  messageLoading.value = false
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || !activeSessionId.value) return
  const result = await sendMessage(activeSessionId.value, content)
  draft.value = ''
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '消息接口不可用，已写入演示会话' : '消息已发送')
  await Promise.all([loadMessages(), loadSessions()])
}

watch(activeSessionId, loadMessages)
onMounted(loadSessions)
</script>

<template>
  <div class="message-page">
    <AppCard title="消息中心" subtitle="查看会话列表与历史消息，支持发送文本消息">
      <el-alert v-if="demoData" title="当前为演示数据模式，消息发送会即时更新会话" type="info" show-icon :closable="false" class="page-feedback" />

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
            <span class="message-session__avatar">{{ session.targetName.slice(0, 1) }}</span>
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
            <span class="message-thread__status">已连接</span>
          </div>

          <el-skeleton v-if="messageLoading" :rows="6" animated />
          <div v-else-if="!messages.length" class="message-empty">暂无历史消息，发送第一条消息吧</div>
          <div v-else class="message-thread__body">
            <article
              v-for="message in messages"
              :key="message.id"
              :class="['message-bubble', { 'message-bubble--mine': message.senderId === 1001 }]"
            >
              <span class="message-bubble__sender">{{ message.senderName }} · {{ message.sentAt }}</span>
              <p>{{ message.content }}</p>
            </article>
          </div>

          <div class="message-composer">
            <el-input v-model="draft" type="textarea" :rows="2" resize="none" placeholder="输入消息，按发送提交" @keyup.ctrl.enter="handleSend" />
            <AppButton variant="primary" :disabled="!draft.trim() || !activeSession" @click="handleSend">发送</AppButton>
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

.message-bubble__sender {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.message-bubble p {
  margin: 0.25rem 0 0;
  line-height: 1.6;
}

.message-composer {
  display: flex;
  align-items: end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
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
