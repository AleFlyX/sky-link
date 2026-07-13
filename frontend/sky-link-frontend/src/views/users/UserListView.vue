<script setup>
import AppCard from '../../components/common/AppCard.vue'
</script>

<template>
  <AppCard
    variant="default"
    title="用户管理"
    subtitle="这里将接入用户列表、状态切换、部门筛选和角色分配功能。"
  >
    <p>这里将接入用户列表、状态切换、部门筛选和角色分配功能。</p>
  </AppCard>
</template>

<style scoped>
p {
  color: var(--color-text-muted);
  margin: 0;
}
</style>
