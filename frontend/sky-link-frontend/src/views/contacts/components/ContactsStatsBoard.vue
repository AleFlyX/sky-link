<script setup>
import AppCard from '../../../components/common/AppCard.vue'

defineProps({
  friendsCount: {
    type: Number,
    default: 0,
  },
  groupsCount: {
    type: Number,
    default: 0,
  },
  incomingRequestsCount: {
    type: Number,
    default: 0,
  },
  pendingIncomingCount: {
    type: Number,
    default: 0,
  },
})

const statItems = [
  { key: 'friendsCount', label: '我的好友' },
  { key: 'groupsCount', label: '加入的群聊' },
  { key: 'incomingRequestsCount', label: '收到的申请' },
  { key: 'pendingIncomingCount', label: '待处理申请' },
]
</script>

<template>
  <div class="contacts-stats-board">
    <AppCard
      v-for="item in statItems"
      :key="item.key"
      variant="ghost"
      padding="sm"
      body-class="contacts-stats-board__card-body"
    >
      <strong>{{ $props[item.key] }}</strong>
      <span>{{ item.label }}</span>
    </AppCard>
  </div>
</template>

<style scoped>
.contacts-stats-board {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.contacts-stats-board :deep(.contacts-stats-board__card-body) {
  display: grid;
  gap: 0.35rem;
  min-height: 6.4rem;
  align-content: start;
}

.contacts-stats-board strong {
  font-size: 1.45rem;
}

.contacts-stats-board span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

@media (max-width: 980px) {
  .contacts-stats-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .contacts-stats-board {
    grid-template-columns: 1fr;
  }
}
</style>
