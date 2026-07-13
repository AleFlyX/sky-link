<script setup>
import {
  Document,
  Bell,
  FolderOpened,
  House,
  Memo,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBrand from '../components/common/AppBrand.vue'
import AppNavItem from '../components/common/AppNavItem.vue'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const navItems = [
  { label: '工作台', to: '/app/dashboard', icon: House },
  { label: '个人中心', to: '/app/profile', icon: User },
  { label: '用户管理', to: '/app/users', icon: User },
  { label: '部门管理', to: '/app/departments', icon: OfficeBuilding },
  { label: '文件中心', to: '/app/files', icon: FolderOpened },
  { label: '任务管理', to: '/app/tasks', icon: Memo },
  { label: '公告通知', to: '/app/notices', icon: Bell },
]

const pageTitle = computed(() => route.meta.title || 'SkyLink')
const pageEyebrow = computed(
  () => route.meta.eyebrow || 'Connected Workspace',
)
const unreadCount = computed(() => appStore.unreadNotificationCount)
const avatarText = computed(() => appStore.currentUser.name.slice(0, 1))

/**
 * 跳转到通知查看页面的时候顺便清空未读消息
 */
function goToNotices() {
  appStore.markNotificationsRead()
  router.push('/app/notices')
}

function goToProfile() {
  router.push('/app/profile')
}
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
          :icon="item.icon"
          :label="item.label"
          :to="item.to"
        />
      </nav>
    </aside>

    <main class="layout__main">
      <header class="layout__header">
        <div class="layout__header-copy">
          <div class="layout__eyebrow">{{ pageEyebrow }}</div>
          <h1 class="layout__title">{{ pageTitle }}</h1>
          <p class="layout__summary">统一沟通、任务、文件与公告的工作台</p>
        </div>

        <div class="layout__header-actions">
          <button
            type="button"
            :class="['layout__notify', { 'layout__notify--active': unreadCount > 0 }]"
            @click="goToNotices"
          >
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <Bell class="layout__notify-icon" />
            </el-badge>
          </button>

          <button type="button" class="layout__profile" @click="goToProfile">
            <el-avatar class="layout__avatar" :size="40">
              {{ avatarText }}
            </el-avatar>
            <div class="layout__profile-copy">
              <strong>{{ appStore.currentUser.name }}</strong>
              <span>{{ appStore.currentUser.roleLabel }}</span>
            </div>
            <Document class="layout__profile-icon" />
          </button>
        </div>
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
  gap: 1rem;
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
  color: var(--color-primary);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.layout__title {
  margin: 0;
  font-size: 1.55rem;
}

.layout__summary {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.layout__header-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.layout__notify,
.layout__profile {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.layout__notify {
  position: relative;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.layout__notify::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.layout__notify--active {
  border-color: rgba(51, 112, 255, 0.25);
  box-shadow: 0 12px 26px rgba(51, 112, 255, 0.16);
}

.layout__notify--active::after {
  opacity: 1;
  background: radial-gradient(circle, rgba(51, 112, 255, 0.18), transparent 65%);
}

.layout__notify:hover,
.layout__profile:hover {
  transform: translateY(-1px);
}

.layout__notify-icon {
  width: 1.1rem;
  height: 1.1rem;
  color: var(--color-primary);
}

.layout__profile {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.45rem 0.75rem 0.45rem 0.5rem;
  border-radius: 18px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.layout__avatar {
  background: linear-gradient(135deg, #77a6ff, #3370ff);
  color: #fff;
  font-weight: 700;
}

.layout__profile-copy {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.layout__profile-copy strong {
  font-size: 0.94rem;
}

.layout__profile-copy span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.layout__profile-icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-text-muted);
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

  .layout__header {
    flex-direction: column;
    align-items: stretch;
  }

  .layout__header-actions {
    justify-content: space-between;
  }

  .layout__profile {
    flex: 1;
  }
}
</style>
