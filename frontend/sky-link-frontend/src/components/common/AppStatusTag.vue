<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: 'default',
  },
})
</script>

<template>
  <span :class="['status-tag', `status-tag--${tone}`]">{{ label }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
}

.status-tag--default,
.status-tag--info {
  background: #eef2f8;
  color: #586174;
}

.status-tag--primary {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.status-tag--success {
  background: #ebf8ef;
  color: #1c8b4d;
}

.status-tag--warning {
  background: #fff5e8;
  color: #c77b11;
}

.status-tag--danger {
  background: #ffecec;
  color: #d64545;
}
</style>
