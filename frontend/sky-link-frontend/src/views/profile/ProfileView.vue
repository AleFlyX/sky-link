<script setup>
import { computed, onMounted } from 'vue'
import AppCard from '../../components/common/AppCard.vue'
import { useUserStore } from '../../stores/user'
import { useAppStore } from '../../stores/app'

const userStore = useUserStore()
const appStore = useAppStore()
const systemIdentity = computed(() => userStore.user.roleLabel || '未分配')
const profileDepartment = computed(() => userStore.user.department || '未加入部门')

onMounted(() => {
  userStore.loadCurrentUser().catch(() => {
    ElMessage.warning('获取个人信息失败，请稍后重试')
  })
})
</script>

<template>
  <div class="page-grid page-grid--profile">
    <AppCard variant="hero" title="个人中心">
      <div class="profile-hero">
        <div class="profile-hero__avatar">{{ userStore.avatarText }}</div>
        <div class="profile-hero__copy">
          <h2>{{ userStore.user.name }}</h2>
          <p>{{ userStore.user.bio }}</p>
          <div class="profile-hero__meta">
            <span>{{ profileDepartment }}</span>
            <span>{{ userStore.user.email }}</span>
          </div>
        </div>
      </div>
    </AppCard>

    <AppCard title="账号信息" class="profile-account-card">
      <div class="info-list">
        <div><strong>账号邮箱</strong><span>{{ userStore.user.email }}</span></div>
        <div><strong>联系电话</strong><span>{{ userStore.user.phone }}</span></div>
        <div><strong>所属部门</strong><span>{{ profileDepartment }}</span></div>
        <div><strong>系统身份</strong><span>{{ systemIdentity }}</span></div>
      </div>
    </AppCard>

    <AppCard title="任务提醒" subtitle="显示待处理任务数量">
      <div class="summary-card">
        <strong>{{ appStore.unreadNotificationCount }} 条</strong>
        <span>待处理任务和提醒</span>
        <AppStatusTag label="待处理" tone="warning" />
      </div>
    </AppCard>
  </div>

  
</template>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.profile-hero__avatar {
  display: grid;
  place-items: center;
  width: 5rem;
  height: 5rem;
  border-radius: 1.4rem;
  background: linear-gradient(145deg, #77a6ff, #3370ff);
  color: #fff;
  font-size: 1.8rem;
  font-weight: 800;
}

.profile-hero__copy h2,
.profile-hero__copy p {
  margin: 0;
}

.profile-hero__copy h2 {
  font-size: 1.45rem;
}

.profile-hero__copy p {
  margin-top: 0.45rem;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.profile-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.85rem;
}

.profile-hero__meta span {
  padding: 0.45rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.profile-account-card {
  grid-column: 1 / -1;
}

.info-list {
  display: grid;
  gap: 0.9rem;
}

.info-list div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border);
}

.info-list strong {
  font-size: 0.92rem;
}

.info-list span {
  color: var(--color-text-muted);
  text-align: right;
}

@media (max-width: 720px) {
  .profile-hero,
  .info-list div {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-list span {
    text-align: left;
  }
}
</style>
