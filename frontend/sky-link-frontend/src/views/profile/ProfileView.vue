<script setup>
import AppCard from '../../components/common/AppCard.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="page-grid page-grid--profile">
    <AppCard variant="hero" title="个人中心" subtitle="个人资料与角色展示统一收口">
      <div class="profile-hero">
        <div class="profile-hero__avatar">{{ userStore.avatarText }}</div>
        <div class="profile-hero__copy">
          <h2>{{ userStore.user.name }}</h2>
          <p>{{ userStore.user.bio }}</p>
          <div class="profile-hero__meta">
            <span>{{ userStore.user.department }}</span>
            <span>{{ userStore.user.email }}</span>
            <span>最近登录：{{ userStore.user.lastLoginAt }}</span>
          </div>
        </div>
      </div>
    </AppCard>

    <AppCard title="账号信息" subtitle="当前已对接个人中心展示所需的基础资料">
      <div class="info-list">
        <div><strong>账号邮箱</strong><span>{{ userStore.user.email }}</span></div>
        <div><strong>联系电话</strong><span>{{ userStore.user.phone }}</span></div>
        <div><strong>所属部门</strong><span>{{ userStore.user.department }}</span></div>
        <div><strong>系统身份</strong><span>{{ userStore.user.roleLabel }}</span></div>
      </div>
    </AppCard>

    <AppCard title="角色展示" subtitle="成员 A 联调项可直接基于这里替换真实数据">
      <div class="role-list">
        <AppStatusTag
          v-for="role in userStore.user.roles"
          :key="role"
          :label="role"
          tone="primary"
        />
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

.info-list,
.role-list {
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

.role-list {
  grid-template-columns: repeat(auto-fit, minmax(120px, max-content));
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
