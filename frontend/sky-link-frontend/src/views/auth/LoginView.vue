<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../api/workspace'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import { useAppStore } from '../../stores/app'
import { setToken } from '../../utils/request'
import { useLoginForm } from './composables/useLoginForm'

const router = useRouter()
const appStore = useAppStore()
const loading = ref(false)
const loginError = ref('')
const { formRef, form, rules, sanitizeForm, getCredentials } = useLoginForm()

async function handleLogin() {
  sanitizeForm()

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  loginError.value = ''
  try {
    const credentials = getCredentials()
    const result = await login(credentials.account, credentials.password)

    if (!result?.token) {
      throw new Error('登录接口未返回有效 token')
    }

    setToken(result.token)
    if (result.userInfo) {
      appStore.currentUser = {
        ...appStore.currentUser,
        id: result.userInfo.userId || result.userInfo.id || appStore.currentUser.id,
        name: result.userInfo.nickname || result.userInfo.name || appStore.currentUser.name,
        account: result.userInfo.username || result.userInfo.account || credentials.account,
        avatar: result.userInfo.avatar || appStore.currentUser.avatar,
        email: result.userInfo.email || appStore.currentUser.email,
        phone: result.userInfo.phone || appStore.currentUser.phone,
        roles: result.userInfo.roles || appStore.currentUser.roles,
      }
    }
    ElMessage.success('登录成功，正在进入工作台')
    await router.push('/app/dashboard')
  } catch (error) {
    loginError.value = error.message || '登录失败，请检查账号和密码'
    ElMessage.error(loginError.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-page__intro">
      <div class="login-page__badge">SkyLink Workspace</div>
      <h1>更轻、更快的团队协作工作台</h1>
      <p>
        面向项目与小型组织的协同平台，把成员、消息、文件、任务和公告统一在一个清晰的工作入口里。
      </p>

      <!-- <div class="login-page__summary">
        <div class="login-page__summary-card">
          <div class="login-page__summary-label">当前版本重点</div>
          <div class="login-page__summary-value">登录、任务、文件、公告</div>
          <div class="login-page__summary-text">优先保证演示路径连贯，界面简洁，操作反馈清晰。</div>
        </div>

        <div class="login-page__grid">
          <article>
            <strong>26</strong>
            <span>核心数据表</span>
          </article>
          <article>
            <strong>9</strong>
            <span>业务模块</span>
          </article>
          <article>
            <strong>3 天</strong>
            <span>冲刺交付</span>
          </article>
        </div>
      </div>

      <div class="login-page__features">
        <div>统一身份与权限管理</div>
        <div>文件、任务与公告集中流转</div>
        <div>适合课程答辩展示的企业化界面</div>
      </div> -->
    </section>

    <section class="login-page__panel">
      <AppCard variant="elevated" padding="lg" class="login-card">
        <div class="login-card__brand">
          <div class="login-card__logo">S</div>
          <div>
            <div class="login-card__title">登录 SkyLink</div>
            <div class="login-card__subtitle">使用团队账号进入工作空间</div>
          </div>
        </div>

        <div class="login-card__switch">账号密码登录</div>

        <el-alert
          v-if="loginError"
          :title="loginError"
          type="error"
          show-icon
          :closable="false"
          class="login-card__feedback"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-card__form"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="账号" prop="account">
            <el-input
              v-model="form.account"
              placeholder="请输入用户名或邮箱"
              size="large"
              clearable
              autocomplete="username"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              autocomplete="current-password"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <div class="login-card__row">
            <el-checkbox v-model="form.remember">7 天内记住我</el-checkbox>
            <a href="/">忘记密码</a>
          </div>

          <AppButton
            variant="primary"
            size="large"
            block
            class="login-card__submit"
            :loading="loading"
            :disabled="loading"
            @click="handleLogin"
          >
            登录工作台
          </AppButton>
        </el-form>

        <div class="login-card__footer">
          <span>首次使用团队账号？</span>
          <a href="/">联系管理员开通</a>
        </div>
      </AppCard>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  min-height: 100vh;
  position: relative;
}

.login-page__intro {
  padding: 4.5rem 4rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-page__badge {
  width: fit-content;
  padding: 0.45rem 0.8rem;
  border: 1px solid rgba(51, 112, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-primary);
  font-size: 0.78rem;
  font-weight: 700;
}

.login-page__intro h1 {
  max-width: 10ch;
  margin: 1.2rem 0 0.9rem;
  font-size: clamp(2.6rem, 4vw, 4rem);
  line-height: 1.12;
  letter-spacing: -0.02em;
}

.login-page__intro p {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 1rem;
  line-height: 1.8;
}

.login-page__summary {
  margin-top: 1.9rem;
}

.login-page__summary-card {
  padding: 1.1rem 1.15rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-card);
}

.login-page__summary-label {
  color: var(--color-primary);
  font-size: 0.78rem;
  font-weight: 700;
}

.login-page__summary-value {
  margin-top: 0.45rem;
  font-size: 1.2rem;
  font-weight: 700;
}

.login-page__summary-text {
  margin-top: 0.45rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.7;
}

.login-page__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
  margin-top: 0.95rem;
}

.login-page__grid article {
  padding: 1rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
}

.login-page__grid strong {
  display: block;
  font-size: 1.05rem;
  margin-bottom: 0.25rem;
}

.login-page__grid span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.login-page__features {
  display: grid;
  gap: 0.7rem;
  margin-top: 1.5rem;
  color: var(--color-text-muted);
}

.login-page__features div {
  position: relative;
  padding-left: 1.2rem;
}

.login-page__features div::before {
  content: '';
  position: absolute;
  top: 0.45rem;
  left: 0;
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 999px;
  background: var(--color-primary);
}

.login-page__panel {
  display: grid;
  place-items: center;
  padding: 2rem;
}

.login-card {
  width: min(460px, 100%);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 42px rgba(31, 35, 41, 0.08);
}

.login-card__brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.login-card__logo {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: linear-gradient(145deg, #4c8dff, #3370ff 65%, #245bdb);
  color: #fff;
  font-size: 1.1rem;
  font-weight: 800;
}

.login-card__title {
  font-size: 1.4rem;
  font-weight: 700;
}

.login-card__subtitle {
  margin-top: 0.2rem;
  color: var(--color-text-muted);
}

.login-card__switch {
  display: inline-flex;
  margin-top: 1.2rem;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
}

.login-card__form {
  margin-top: 1.5rem;
}

.login-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0.15rem 0 1.2rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.login-card__row a,
.login-card__footer a {
  color: var(--color-primary);
  font-weight: 600;
}

.login-card__submit {
  width: 100%;
  height: 46px;
  font-weight: 700;
}

.login-card__footer {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  margin-top: 1.25rem;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-page__intro {
    padding: 3rem 1.5rem 1rem;
  }

  .login-page__grid {
    grid-template-columns: 1fr;
  }

  .login-page__panel {
    padding: 1rem 1.5rem 2rem;
  }
}
</style>
