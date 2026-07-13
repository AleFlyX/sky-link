<template>
  <div class="brand">
    <div class="brand__mark">S</div>
    <div>
      <div class="brand__name">SkyLink</div>
      <div class="brand__sub">Team Workspace</div>
    </div>
  </div>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
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
}

.brand__name {
  font-size: 1rem;
  font-weight: 700;
}

.brand__sub {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
</style>
