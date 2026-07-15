<script setup>
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import MessageComposer from './components/MessageComposer.vue'
import MessageSessionList from './components/MessageSessionList.vue'
import { useMessageCenter } from './composables/useMessageCenter'

const {
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
} = useMessageCenter()
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
        <MessageSessionList
          :sessions="sessions"
          :loading="loading"
          :active-session-id="activeSessionId"
          @select="selectSession"
        />

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
              :class="[
                'message-bubble',
                { 'message-bubble--emoji': message.messageType === 'emoji' && !message.recalled },
                { 'message-bubble--mine': message.senderId === currentUserId },
              ]"
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

          <MessageComposer
            v-model:draft="draft"
            v-model:emojiPopoverVisible="emojiPopoverVisible"
            :active-session="activeSession"
            :emoji-options="emojiOptions"
            @send-text="handleSend"
            @send-emoji="handleSendEmoji"
          />
        </section>
      </div>
    </AppCard>
  </div>
</template>

<style scoped>
.message-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.34fr) minmax(0, 1fr);
  height: 70vh;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.message-thread {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.message-thread__header > div {
  display: grid;
  gap: 0.2rem;
}

.message-thread__header span,
.message-thread__status {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.message-thread__body {
  flex: 1;
  min-height: 0;
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

.message-bubble--emoji {
  max-width: min(48%, 18rem);
}

.message-bubble--mine {
  justify-self: end;
  border-radius: 0.8rem 0.8rem 0.2rem 0.8rem;
  background: var(--color-primary-soft);
}

.message-bubble--emoji p {
  margin-top: 0.1rem;
  font-size: 2rem;
  line-height: 1.1;
  text-align: center;
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
}
</style>
