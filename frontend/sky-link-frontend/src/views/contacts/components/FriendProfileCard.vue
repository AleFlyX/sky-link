<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppStatusTag from '../../../components/common/AppStatusTag.vue'

defineProps({
  friend: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open-chat'])

function getStatusTone(status) {
  if (status === '在线' || status === '启用') {
    return 'success'
  }
  if (status === '忙碌') {
    return 'warning'
  }
  if (status === '离线' || status === '禁用') {
    return 'info'
  }
  return 'primary'
}
</script>

<template>
  <AppCard
    variant="soft"
    padding="sm"
    interactive
    body-class="friend-profile-card"
  >
    <div class="friend-profile-card__main">
      <div class="friend-profile-card__header">
        <span class="friend-profile-card__avatar">{{ friend.name.slice(0, 1) }}</span>
        <div class="friend-profile-card__identity">
          <strong>{{ friend.name }}</strong>
          <span>@{{ friend.account }}</span>
        </div>
      </div>
      <div class="friend-profile-card__details">
        <div class="friend-profile-card__meta">
          <span class="friend-profile-card__chip">{{ friend.department }}</span>
          <AppStatusTag :label="friend.status" :tone="getStatusTone(friend.status)" />
          <span class="friend-profile-card__chip friend-profile-card__chip--muted">{{ friend.lastSeen }}</span>
        </div>
      </div>
    </div>
    <div class="friend-profile-card__actions">
      <AppButton size="small" variant="primary" @click="emit('open-chat', friend)">发消息</AppButton>
    </div>
  </AppCard>
</template>

<style scoped>
:deep(.friend-profile-card) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background:
    radial-gradient(circle at top right, rgba(51, 112, 255, 0.08), transparent 40%),
    linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.friend-profile-card__main {
  display: grid;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}

.friend-profile-card__header {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  min-width: 0;
}

.friend-profile-card__avatar {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 1rem;
  font-weight: 800;
}

.friend-profile-card__identity {
  display: grid;
  gap: 0.15rem;
  min-width: 0;
}

.friend-profile-card__identity strong {
  font-size: 1rem;
}

.friend-profile-card__identity strong,
.friend-profile-card__identity span,
.friend-profile-card__meta span,
.friend-profile-card__hint {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-profile-card__details {
  min-width: 0;
}

.friend-profile-card__identity span,
.friend-profile-card__meta {
  color: var(--color-text-muted);
}

.friend-profile-card__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  min-width: 0;
}

.friend-profile-card__chip {
  display: inline-flex;
  align-items: center;
  max-width: 11rem;
  min-width: 0;
  padding: 0.28rem 0.6rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-muted);
  font-size: 0.8rem;
  line-height: 1;
}

.friend-profile-card__chip--muted {
  background: rgba(248, 250, 252, 0.9);
}

.friend-profile-card__chip,
.friend-profile-card__chip :deep(*) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-profile-card__meta :deep(.status-tag) {
  min-width: auto;
  padding: 0.28rem 0.6rem;
  font-size: 0.8rem;
  line-height: 1;
}

.friend-profile-card__actions {
  display: flex;
  flex: 0 0 auto;
}

@media (max-width: 760px) {
  :deep(.friend-profile-card) {
    align-items: stretch;
    flex-direction: column;
  }

  .friend-profile-card__actions {
    width: 100%;
  }

  .friend-profile-card__actions :deep(.app-button) {
    width: 100%;
  }
}
</style>
