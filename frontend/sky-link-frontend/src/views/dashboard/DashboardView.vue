<script setup>
import AppCard from '../../components/common/AppCard.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()

const cards = [
  { title: '用户与组织', text: '个人中心、用户管理、部门管理已能承接联调。' },
  { title: '协作业务', text: '文件、任务、公告三条列表链路已经串通。' },
  { title: '公共组件', text: '表格、分页、表单弹窗、状态标签已统一沉淀。' },
]
</script>

<template>
  <div class="dashboard">
    <AppCard variant="hero" title="Day 2 前端联调面板" padding="lg">
      <div>
        <p class="hero__eyebrow">Sprint Snapshot</p>
        <p class="hero__text">
          当前工作台已经具备可演示的头栏、通知提醒、列表页公共能力和多业务页面骨架，后续主要替换为真实接口。
        </p>
      </div>
    </AppCard>

    <section class="summary-grid">
      <AppCard title="当前登录成员" subtitle="头栏与个人中心共享同一份用户资料">
        <div class="summary-card">
          <strong>{{ appStore.currentUser.name }}</strong>
          <span>{{ appStore.currentUser.department }}</span>
          <AppStatusTag :label="appStore.currentUser.roleLabel" tone="primary" />
        </div>
      </AppCard>

      <AppCard title="未读提醒" subtitle="铃铛按钮会随着这里的数据变化一起响应">
        <div class="summary-card">
          <strong>{{ appStore.unreadNotificationCount }} 条</strong>
          <span>含公告提醒与任务状态更新</span>
          <AppStatusTag label="待处理" tone="warning" />
        </div>
      </AppCard>
    </section>

    <section class="cards">
      <AppCard
        v-for="card in cards"
        :key="card.title"
        variant="soft"
        :title="card.title"
      >
        <p>{{ card.text }}</p>
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

.hero__eyebrow {
  margin: 0 0 0.65rem;
  color: var(--color-primary);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.8rem;
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
