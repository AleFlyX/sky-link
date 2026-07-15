<script setup>
import AppCard from '../../../components/common/AppCard.vue'

defineProps({
  incomingCount: {
    type: Number,
    default: 0,
  },
  outgoingCount: {
    type: Number,
    default: 0,
  },
  pendingCount: {
    type: Number,
    default: 0,
  },
})
</script>

<template>
  <div class="friend-request-overview">
    <AppCard variant="soft" padding="sm" body-class="friend-request-overview__body">
      <strong>{{ incomingCount }}</strong>
      <span>收到的申请</span>
    </AppCard>
    <AppCard variant="soft" padding="sm" body-class="friend-request-overview__body">
      <strong>{{ outgoingCount }}</strong>
      <span>我发出的申请</span>
    </AppCard>
    <AppCard variant="hero" padding="sm" body-class="friend-request-overview__body">
      <strong>{{ pendingCount }}</strong>
      <span>待处理申请</span>
    </AppCard>
  </div>
</template>

<style scoped>
.friend-request-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.friend-request-overview :deep(.friend-request-overview__body) {
  display: grid;
  gap: 0.3rem;
}

.friend-request-overview strong {
  font-size: 1.35rem;
}

.friend-request-overview span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

@media (max-width: 760px) {
  .friend-request-overview {
    grid-template-columns: 1fr;
  }
}
</style>
