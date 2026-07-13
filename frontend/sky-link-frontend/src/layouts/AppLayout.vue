<script setup>
import AppBrand from '../components/common/AppBrand.vue'
import AppNavItem from '../components/common/AppNavItem.vue'

const navItems = [
  { label: '工作台', to: '/app/dashboard' },
  { label: '用户管理', to: '/app/users' },
  { label: '文件中心', to: '/app/files' },
  { label: '任务管理', to: '/app/tasks' },
  { label: '公告通知', to: '/app/notices' },
]
</script>

<template>
  <div class="layout">
    <aside class="layout__sidebar">
      <div class="layout__sidebar-top">
        <AppBrand />
        <div class="layout__workspace">SkyLink 团队空间</div>
      </div>

      <nav class="layout__nav">
        <AppNavItem
          v-for="item in navItems"
          :key="item.to"
          :label="item.label"
          :to="item.to"
        />
      </nav>
    </aside>

    <main class="layout__main">
      <header class="layout__header">
        <div class="layout__header-copy">
          <h1 class="layout__title">协作办公平台</h1>
          <div class="layout__eyebrow">统一沟通、任务、文件与公告的工作台</div>
        </div>
        <div class="layout__status">Workspace Ready</div>
      </header>

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
  min-height: 100vh;
}

.layout__sidebar {
  padding: 1.25rem;
  border-right: 1px solid var(--color-border);
  background: rgba(250, 252, 255, 0.88);
  backdrop-filter: blur(10px);
}

.layout__sidebar-top {
  padding: 0.35rem 0.35rem 0.95rem;
}

.layout__workspace {
  margin-top: 0.9rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.layout__nav {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.layout__main {
  padding: 1.25rem;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-card);
}

.layout__header-copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.layout__eyebrow {
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.layout__title {
  margin: 0;
  font-size: 1.55rem;
}

.layout__status {
  padding: 0.55rem 0.85rem;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
}

.layout__content {
  min-height: calc(100vh - 8rem);
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .layout__sidebar {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }

  .layout__nav {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
