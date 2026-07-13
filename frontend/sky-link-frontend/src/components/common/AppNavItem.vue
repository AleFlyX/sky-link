<script setup>
defineProps({
  to: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <RouterLink :to="to" class="nav-item">
    <span class="nav-item__dot"></span>
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

.nav-item__dot {
  width: 0.42rem;
  height: 0.42rem;
  border-radius: 999px;
  background: #c6d2e4;
  transition: 0.2s ease;
}

.nav-item.router-link-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-item.router-link-active .nav-item__dot {
  background: var(--color-primary);
}

.nav-item:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}
</style>
