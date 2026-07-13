<script setup>
import AppCard from '../../components/common/AppCard.vue'

const cards = [
  { title: '用户与组织', text: '登录、用户、部门和角色权限是第一批联调重点。' },
  { title: '协作业务', text: '文件、任务、公告和消息会在这里逐步接入真实接口。' },
  { title: '前端结构', text: '当前目录已按标准结构初始化，方便三人并行开发。' },
]
</script>

<template>
  <div class="dashboard">
    <AppCard variant="hero" title="前端基础结构已完成初始化" padding="lg">
      <div>
        <p class="hero__eyebrow">Project Ready</p>
        <p class="hero__text">
          现在可以直接在 `views/` 中补页面，在 `api/` 中补接口，在 `components/` 中沉淀复用组件。
        </p>
      </div>
    </AppCard>

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
  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
