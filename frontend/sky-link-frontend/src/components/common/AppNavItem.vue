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
  collapsed: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <RouterLink
    :to="to"
    class="nav-item"
    :class="{ 'nav-item--collapsed': collapsed }"
    :title="label"
  >
    <ElIcon v-if="icon" class="nav-item__icon">
      <component :is="icon" />
    </ElIcon>
    <span class="nav-item__label">{{ label }}</span>
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
  transition:
    0.2s ease,
    padding 0.28s ease,
    gap 0.28s ease,
    justify-content 0.28s ease;
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

.nav-item__label {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  transition:
    max-width 0.28s ease,
    opacity 0.2s ease,
    transform 0.28s ease;
  max-width: 6rem;
  opacity: 1;
  transform: translateX(0);
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

.nav-item--collapsed {
  justify-content: center;
  gap: 0;
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}

.nav-item--collapsed .nav-item__label {
  max-width: 0;
  opacity: 0;
  transform: translateX(-0.35rem);
}

@media (max-width: 900px) {
  .nav-item--collapsed {
    justify-content: flex-start;
    gap: 0.7rem;
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .nav-item--collapsed .nav-item__label {
    max-width: 6rem;
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
