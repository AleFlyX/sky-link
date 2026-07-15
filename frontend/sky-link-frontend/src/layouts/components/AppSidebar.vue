<script setup>
import {
  ChatDotRound,
  DArrowLeft,
  DArrowRight,
  Document,
  House,
  Memo,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import { ElIcon } from 'element-plus'
import AppBrand from '@/components/common/AppBrand.vue'
import AppNavItem from '@/components/common/AppNavItem.vue'

defineProps({
  collapsed: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['toggle'])

const navItems = [
  { label: '工作台', to: '/app/dashboard', icon: House },
  { label: '个人中心', to: '/app/profile', icon: User },
  { label: '用户管理', to: '/app/users', icon: User },
  { label: '部门管理', to: '/app/departments', icon: OfficeBuilding },
  { label: '任务管理', to: '/app/tasks', icon: Memo },
  { label: '通讯录', to: '/app/contacts', icon: User },
  { label: '消息中心', to: '/app/messages', icon: ChatDotRound },
  { label: '在线文档', to: '/app/documents', icon: Document },
]
</script>

<template>
  <aside
    class="app-sidebar"
    :class="{ 'app-sidebar--collapsed': collapsed }"
  >
    <div class="app-sidebar__top">
      <div class="app-sidebar__top-bar">
        <AppBrand :collapsed="collapsed" />
        <button
          class="app-sidebar__toggle"
          type="button"
          :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="$emit('toggle')"
        >
          <ElIcon class="app-sidebar__toggle-icon">
            <component :is="collapsed ? DArrowRight : DArrowLeft" />
          </ElIcon>
        </button>
      </div>
      <Transition name="sidebar-workspace">
        <div v-if="!collapsed" class="app-sidebar__workspace">
          SkyLink 团队空间
        </div>
      </Transition>
    </div>

    <nav class="app-sidebar__nav">
      <AppNavItem
        v-for="item in navItems"
        :key="item.to"
        :collapsed="collapsed"
        :icon="item.icon"
        :label="item.label"
        :to="item.to"
      />
    </nav>
  </aside>
</template>

<style scoped>
.app-sidebar {
  position: sticky;
  top: 0;
  box-sizing: border-box;
  height: 100vh;
  overflow-y: auto;
  padding: 1.25rem;
  border-right: 1px solid var(--color-border);
  background: rgba(250, 252, 255, 0.88);
  backdrop-filter: blur(10px);
  transition:
    padding 0.28s ease,
    background-color 0.28s ease,
    border-color 0.28s ease;
}

.app-sidebar__top {
  padding: 0.35rem 0.35rem 0.95rem;
  transition: padding 0.28s ease;
}

.app-sidebar__top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  transition: gap 0.28s ease;
}

.app-sidebar__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    transform 0.28s ease;
}

.app-sidebar__toggle-icon {
  transition: transform 0.28s ease;
}

.app-sidebar__toggle:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.app-sidebar--collapsed .app-sidebar__toggle {
  transform: translateY(-0.1rem);
}

.app-sidebar__workspace {
  margin-top: 0.9rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.sidebar-workspace-enter-active,
.sidebar-workspace-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.28s ease,
    max-height 0.28s ease,
    margin-top 0.28s ease,
    padding 0.28s ease;
  overflow: hidden;
}

.sidebar-workspace-enter-from,
.sidebar-workspace-leave-to {
  opacity: 0;
  transform: translateY(-0.35rem);
  max-height: 0;
  margin-top: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.sidebar-workspace-enter-to,
.sidebar-workspace-leave-from {
  opacity: 1;
  transform: translateY(0);
  max-height: 6rem;
}

.app-sidebar--collapsed .app-sidebar__top {
  padding-left: 0;
  padding-right: 0;
}

.app-sidebar--collapsed .app-sidebar__top-bar {
  flex-direction: column;
}

@media (max-width: 900px) {
  .app-sidebar {
    position: static;
    height: auto;
    overflow: visible;
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }

  .app-sidebar__nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .app-sidebar--collapsed .app-sidebar__top {
    padding-left: 0.35rem;
    padding-right: 0.35rem;
  }

  .app-sidebar--collapsed .app-sidebar__top-bar {
    flex-direction: row;
  }
}
</style>
