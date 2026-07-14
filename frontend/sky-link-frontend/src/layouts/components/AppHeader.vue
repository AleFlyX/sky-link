<script setup>
import { Bell } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../../stores/app'
import AppProfileDropdown from './AppProfileDropdown.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const pageTitle = computed(() => route.meta.title || 'SkyLink')
const pageEyebrow = computed(
  () => route.meta.eyebrow || 'Connected Workspace',
)
const unreadCount = computed(() => appStore.unreadNotificationCount)

function goToTasks() {
  appStore.markNotificationsRead()
  router.push('/app/tasks')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__copy">
      <div class="app-header__eyebrow">{{ pageEyebrow }}</div>
      <h1 class="app-header__title">{{ pageTitle }}</h1>
      <p class="app-header__summary">统一沟通、任务与文档的协作工作台</p>
    </div>

    <div class="app-header__actions">
      <button
        type="button"
        :class="['app-header__notify', { 'app-header__notify--active': unreadCount > 0 }]"
        @click="goToTasks"
      >
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <Bell class="app-header__notify-icon" />
        </el-badge>
      </button>

      <AppProfileDropdown class="app-header__profile-menu" />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-card);
}

.app-header__copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.app-header__eyebrow {
  color: var(--color-primary);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-header__title {
  margin: 0;
  font-size: 1.55rem;
}

.app-header__summary {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.app-header__notify {
  position: relative;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: var(--color-surface);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.app-header__notify::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-header__notify--active {
  border-color: rgba(51, 112, 255, 0.25);
  box-shadow: 0 12px 26px rgba(51, 112, 255, 0.16);
}

.app-header__notify--active::after {
  opacity: 1;
  background: radial-gradient(circle, rgba(51, 112, 255, 0.18), transparent 65%);
}

.app-header__notify:hover {
  transform: translateY(-1px);
}

.app-header__notify-icon {
  width: 1.1rem;
  height: 1.1rem;
  color: var(--color-primary);
}

@media (max-width: 900px) {
  .app-header {
    flex-direction: column;
    align-items: stretch;
  }

  .app-header__actions {
    justify-content: space-between;
  }

  .app-header__profile-menu {
    flex: 1;
  }

  .app-header__profile-menu :deep(.profile-dropdown) {
    width: 100%;
  }
}
</style>
