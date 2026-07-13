<script setup>
import AppCard from '../../components/common/AppCard.vue'
</script>

<template>
  <AppCard
    variant="default"
    title="任务管理"
    subtitle="这里将接入任务列表、任务详情、状态更新和附件展示。"
  >
    <p>这里将接入任务列表、任务详情、状态更新和附件展示。</p>
  </AppCard>
</template>

<style scoped>
p {
  color: var(--color-text-muted);
  margin: 0;
}
</style>
