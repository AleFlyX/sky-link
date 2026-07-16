<script setup>
import { ref } from 'vue'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'

const isSidebarCollapsed = ref(false)
</script>

<template>
  <div class="layout" :class="{ 'layout--sidebar-collapsed': isSidebarCollapsed }">
    <AppSidebar
      :collapsed="isSidebarCollapsed"
      @toggle="isSidebarCollapsed = !isSidebarCollapsed"
    />

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
  transition: grid-template-columns 0.28s ease;
}

.layout--sidebar-collapsed {
  grid-template-columns: 88px 1fr;
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

  .layout--sidebar-collapsed {
    grid-template-columns: 1fr;
  }
}
</style>
