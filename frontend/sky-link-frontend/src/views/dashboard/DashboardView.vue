<script setup>
import AppCard from '../../components/common/AppCard.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useAppStore } from '../../stores/app'
// import { useUserStore } from '../../stores/user'

const appStore = useAppStore()
// const userStore = useUserStore()

// const cards = [
//   { title: '用户与组织', text: '包含个人中心、用户管理和部门管理。' },
//   { title: '协作业务', text: '包含通讯录、消息、文档和任务功能。' },
//   { title: '公共组件', text: '已提供表格、分页、表单弹窗和状态标签。' },
// ]
</script>

<template>
  <div class="dashboard">


    <section class="summary-grid">


      <AppCard title="任务提醒" subtitle="显示待处理任务数量">
        <div class="summary-card">
          <strong>{{ appStore.unreadNotificationCount }} 条</strong>
          <span>待处理任务和提醒</span>
          <AppStatusTag label="待处理" tone="warning" />
        </div>
      </AppCard>
    </section>

    
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 1.25rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.summary-card {
  display: grid;
  gap: 0.6rem;
}

.summary-card strong {
  font-size: 1.35rem;
}

.summary-card span {
  color: var(--color-text-muted);
}

.hero__text,
p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 900px) {
  .summary-grid,
  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
