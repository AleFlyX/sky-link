<script setup>
import { ElIcon } from 'element-plus'

defineProps({
  to: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  icon: {
    type: Object,
    default: null,
  },
})
</script>

<template>
  <RouterLink :to="to" class="nav-item">
    <ElIcon v-if="icon" class="nav-item__icon">
      <component :is="icon" />
    </ElIcon>
    <span>{{ label }}</span>
  </RouterLink>
</template>

<style scoped>
.nav-item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.8rem 1rem;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  transition: 0.2s ease;
}

.nav-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  font-size: 1rem;
  transition: 0.2s ease;
}

.nav-item__icon :deep(svg) {
  display: block;
  width: 1rem;
  height: 1rem;
}

.nav-item.router-link-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-item:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}
</style>
