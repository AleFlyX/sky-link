<script setup>
import {
  Bell,
  Calendar,
  ChatDotRound,
  Document,
  FolderOpened,
  House,
  Memo,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import AppBrand from '@/components/common/AppBrand.vue'
import AppNavItem from '@/components/common/AppNavItem.vue'

const navItems = [
  { label: '工作台', to: '/app/dashboard', icon: House },
  { label: '个人中心', to: '/app/profile', icon: User },
  { label: '用户管理', to: '/app/users', icon: User },
  { label: '部门管理', to: '/app/departments', icon: OfficeBuilding },
  { label: '文件中心', to: '/app/files', icon: FolderOpened },
  { label: '任务管理', to: '/app/tasks', icon: Memo },
  { label: '公告通知', to: '/app/notices', icon: Bell },
  { label: '通讯录', to: '/app/contacts', icon: User },
  { label: '消息中心', to: '/app/messages', icon: ChatDotRound },
  { label: '在线文档', to: '/app/documents', icon: Document },
  { label: '日程安排', to: '/app/schedules', icon: Calendar },
]
</script>

<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__top">
      <AppBrand />
      <div class="app-sidebar__workspace">SkyLink 团队空间</div>
    </div>

    <nav class="app-sidebar__nav">
      <AppNavItem
        v-for="item in navItems"
        :key="item.to"
        :icon="item.icon"
        :label="item.label"
        :to="item.to"
      />
    </nav>
  </aside>
</template>

<style scoped>
.app-sidebar {
  padding: 1.25rem;
  border-right: 1px solid var(--color-border);
  background: rgba(250, 252, 255, 0.88);
  backdrop-filter: blur(10px);
}

.app-sidebar__top {
  padding: 0.35rem 0.35rem 0.95rem;
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

@media (max-width: 900px) {
  .app-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }

  .app-sidebar__nav {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
