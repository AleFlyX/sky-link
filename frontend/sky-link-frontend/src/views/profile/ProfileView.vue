<script setup>
import AppCard from '../../components/common/AppCard.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()
</script>

<template>
  <div class="page-grid page-grid--profile">
    <AppCard variant="hero" title="个人中心" subtitle="个人资料、角色展示与最近通知统一收口">
      <div class="profile-hero">
        <div class="profile-hero__avatar">{{ appStore.currentUser.name.slice(0, 1) }}</div>
        <div class="profile-hero__copy">
          <h2>{{ appStore.currentUser.name }}</h2>
          <p>{{ appStore.currentUser.bio }}</p>
          <div class="profile-hero__meta">
            <span>{{ appStore.currentUser.department }}</span>
            <span>{{ appStore.currentUser.email }}</span>
            <span>最近登录：{{ appStore.currentUser.lastLoginAt }}</span>
          </div>
        </div>
      </div>
    </AppCard>

    <AppCard title="账号信息" subtitle="当前已对接个人中心展示所需的基础资料">
      <div class="info-list">
        <div><strong>账号邮箱</strong><span>{{ appStore.currentUser.email }}</span></div>
        <div><strong>联系电话</strong><span>{{ appStore.currentUser.phone }}</span></div>
        <div><strong>所属部门</strong><span>{{ appStore.currentUser.department }}</span></div>
        <div><strong>系统身份</strong><span>{{ appStore.currentUser.roleLabel }}</span></div>
      </div>
    </AppCard>

    <AppCard title="角色展示" subtitle="成员 A 联调项可直接基于这里替换真实数据">
      <div class="role-list">
        <AppStatusTag
          v-for="role in appStore.currentUser.roles"
          :key="role"
          :label="role"
          tone="primary"
        />
      </div>
    </AppCard>

    <AppCard title="通知概览" subtitle="铃铛按钮和这里共用同一份未读通知数据">
      <div class="notice-list">
        <article
          v-for="item in appStore.notifications"
          :key="item.id"
          :class="['notice-item', { 'notice-item--unread': !item.read }]"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.time }}</span>
        </article>
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
.role-list,
.notice-list {
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

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 16px;
}

.notice-item strong {
  font-size: 0.95rem;
}

.notice-item span {
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.notice-item--unread {
  border-color: rgba(51, 112, 255, 0.26);
  background: rgba(51, 112, 255, 0.05);
}

@media (max-width: 720px) {
  .profile-hero,
  .info-list div,
  .notice-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-list span {
    text-align: left;
  }
}
</style>
