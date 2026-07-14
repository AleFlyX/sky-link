<script setup>
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'
</script>

<template>
  <div class="layout">
    <AppSidebar />

    <main class="layout__main">
      <AppHeader />

      <section class="layout__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 248px 1fr;
  align-items: start;
  min-height: 100vh;
}

.layout__main {
  padding: 1.25rem;
}

.layout__content {
  min-height: calc(100vh - 8rem);
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
