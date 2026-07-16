<script setup>
import AppCard from '../../components/common/AppCard.vue'
</script>

<template>
  <div class="error-page">
    <section class="error-page__intro">
      <div class="error-page__badge">401</div>
      <h1>当前访问未通过身份校验</h1>
      <p>
        登录状态可能已失效，或当前账号暂时没有访问该页面的权限。请重新登录，或联系管理员确认角色配置。
      </p>

      <div class="error-page__tips">
        <div>请重新登录后再试</div>
        <div>如仍无权限，请联系管理员</div>
      </div>
    </section>

    <section class="error-page__panel">
      <AppCard variant="elevated" padding="lg" class="error-card">
        <div class="error-card__code">401</div>
        <div class="error-card__title">无权访问当前页面</div>
        <p class="error-card__text">
          当前登录状态无效，或账号没有对应权限。请重新登录后再试。
        </p>

        <div class="error-card__actions">
          <router-link to="/login">
            <el-button type="primary" size="large">返回登录</el-button>
          </router-link>
          <router-link to="/app/profile">
            <el-button size="large" plain>前往首页</el-button>
          </router-link>
        </div>
      </AppCard>
    </section>
  </div>
</template>

<style scoped>
.error-page {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  min-height: 100vh;
}

.error-page__intro {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4.5rem 4rem;
}

.error-page__badge {
  width: fit-content;
  padding: 0.45rem 0.8rem;
  border: 1px solid rgba(51, 112, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-primary);
  font-size: 0.78rem;
  font-weight: 700;
}

.error-page__intro h1 {
  max-width: 11ch;
  margin: 1.15rem 0 0.85rem;
  font-size: clamp(2.4rem, 4vw, 3.8rem);
  line-height: 1.14;
  letter-spacing: -0.02em;
}

.error-page__intro p {
  max-width: 33rem;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 1rem;
  line-height: 1.8;
}

.error-page__tips {
  display: grid;
  gap: 0.75rem;
  margin-top: 1.6rem;
}

.error-page__tips div {
  position: relative;
  padding: 1rem 1rem 1rem 1.3rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-muted);
}

.error-page__tips div::before {
  content: '';
  position: absolute;
  top: 1.25rem;
  left: 0.75rem;
  width: 0.38rem;
  height: 0.38rem;
  border-radius: 999px;
  background: var(--color-primary);
}

.error-page__panel {
  display: grid;
  place-items: center;
  padding: 2rem;
}

.error-card {
  width: min(480px, 100%);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
}

.error-card__code {
  font-size: clamp(3.8rem, 9vw, 5.5rem);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--color-primary);
}

.error-card__title {
  margin-top: 1rem;
  font-size: 1.5rem;
  font-weight: 700;
}

.error-card__text {
  margin: 0.85rem 0 0;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.error-card__actions {
  display: flex;
  gap: 0.85rem;
  flex-wrap: wrap;
  margin-top: 1.7rem;
}

@media (max-width: 980px) {
  .error-page {
    grid-template-columns: 1fr;
  }

  .error-page__intro {
    padding: 3rem 1.5rem 1rem;
  }

  .error-page__panel {
    padding: 1rem 1.5rem 2rem;
  }

  .error-card__actions {
    flex-direction: column;
  }
}
</style>
