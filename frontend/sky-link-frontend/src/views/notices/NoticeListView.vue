<script setup>
import AppCard from '../../components/common/AppCard.vue'
</script>

<template>
  <AppCard
    variant="default"
    title="公告通知"
    subtitle="这里将接入公告列表、未读数量和公告详情查看。"
  >
    <p>这里将接入公告列表、未读数量和公告详情查看。</p>
  </AppCard>
</template>

<style scoped>
p {
  color: var(--color-text-muted);
  margin: 0;
}
</style>
