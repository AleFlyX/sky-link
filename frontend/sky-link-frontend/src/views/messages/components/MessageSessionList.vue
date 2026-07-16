<script setup>
import { ElSkeleton } from 'element-plus'

defineProps({
  activeSessionId: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  sessions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['select'])
</script>

<template>
  <aside class="message-sessions">
    <div class="message-sessions__heading">
      <strong>最近会话</strong>
      <span>{{ sessions.length }}</span>
    </div>
    <div class="message-sessions__body">
      <el-skeleton v-if="loading" :rows="5" animated />
      <template v-else>
        <button
          v-for="session in sessions"
          :key="session.id"
          type="button"
          :class="[
            'message-session',
            { 'message-session--active': session.id === activeSessionId },
          ]"
          @click="emit('select', session.id)"
        >
          <span class="message-session__avatar">{{ session.targetName?.slice(0, 1) || '?' }}</span>
          <span class="message-session__copy">
            <strong>{{ session.targetName }}</strong>
            <small>{{ session.lastMessage }}</small>
          </span>
        </button>
        <div v-if="!sessions.length" class="message-empty">暂无会话</div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.message-sessions {
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 1rem;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface-muted);
  overflow: hidden;
}

.message-sessions__heading {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  color: var(--color-text);
}

.message-sessions__heading span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.message-sessions__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
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

.message-empty {
  display: grid;
  place-items: center;
  min-height: 8rem;
  color: var(--color-text-muted);
}
</style>
