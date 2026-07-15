<script setup>
defineProps({
  collapsed: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div class="brand" :class="{ 'brand--collapsed': collapsed }">
    <div class="brand__mark">S</div>
    <div class="brand__content">
      <div class="brand__name">SkyLink</div>
      <div class="brand__sub">团队协作系统</div>
    </div>
  </div>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: gap 0.28s ease;
}

.brand__mark {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.9rem;
  background:
    linear-gradient(145deg, #4c8dff, #3370ff 62%, #245bdb);
  color: #fff;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.34);
  flex-shrink: 0;
}

.brand__content {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  transition:
    max-width 0.28s ease,
    opacity 0.2s ease,
    transform 0.28s ease;
  max-width: 8rem;
  opacity: 1;
  transform: translateX(0);
}

.brand__name {
  font-size: 1rem;
  font-weight: 700;
}

.brand__sub {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.brand--collapsed {
  justify-content: center;
  gap: 0;
}

.brand--collapsed .brand__content {
  max-width: 0;
  opacity: 0;
  transform: translateX(-0.35rem);
}
</style>
