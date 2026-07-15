<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppInput from '../../components/common/AppInput.vue'
import { register } from '../../api/workspace'
import { useRegisterForm } from './composables/useRegisterForm'

const router = useRouter()
const loading = ref(false)
const registerError = ref('')
const { formRef, form, rules, sanitizeForm, getPayload } = useRegisterForm()

async function handleRegister() {
  sanitizeForm()

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  registerError.value = ''
  try {
    const payload = getPayload()
    await register(payload)
    ElMessage.success('注册成功，请登录')
    await router.push({
      path: '/login',
      query: {
        account: payload.username,
        registered: '1',
      },
    })
  } catch (error) {
    registerError.value = error.message || '注册失败，请稍后重试'
    ElMessage.error(registerError.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <section class="register-page__intro">
      <div class="register-page__badge">账号注册</div>
      <h1>注册账号</h1>
      <p>
        填写基础信息后即可登录系统。
      </p>
    </section>

    <section class="register-page__panel">
      <AppCard variant="elevated" padding="lg" class="register-card">
        <div class="register-card__brand">
          <div class="register-card__logo">S</div>
          <div>
            <div class="register-card__title">注册 SkyLink</div>
            <div class="register-card__subtitle">请填写账号信息</div>
          </div>
        </div>

        <div class="register-card__switch">注册</div>

        <el-alert
          v-if="registerError"
          :title="registerError"
          type="error"
          show-icon
          :closable="false"
          class="register-card__feedback"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="register-card__form"
          @submit.prevent="handleRegister"
        >
          <el-form-item label="账号" prop="username">
            <AppInput
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              clearable
              autocomplete="username"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <AppInput
              v-model="form.nickname"
              placeholder="不填则默认使用账号名"
              size="large"
              clearable
              autocomplete="nickname"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <AppInput
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
              clearable
              autocomplete="email"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <AppInput
              v-model="form.phone"
              placeholder="请输入 11 位手机号"
              size="large"
              clearable
              autocomplete="tel"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <AppInput
              v-model="form.password"
              type="password"
              placeholder="至少 8 位，需包含字母和数字"
              size="large"
              show-password
              autocomplete="new-password"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <AppInput
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
              autocomplete="new-password"
              @blur="sanitizeForm"
            />
          </el-form-item>

          <AppButton
            variant="primary"
            size="large"
            block
            class="register-card__submit"
            :loading="loading"
            :disabled="loading"
            @click="handleRegister"
          >
            创建账号
          </AppButton>
        </el-form>

        <div class="register-card__footer">
          <span>已经有账号了？</span>
          <RouterLink to="/login">返回登录</RouterLink>
        </div>
      </AppCard>
    </section>
  </div>
</template>

<style scoped>
.register-page {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  min-height: 100vh;
  position: relative;
}

.register-page__intro {
  padding: 4.5rem 4rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-page__badge {
  width: fit-content;
  padding: 0.45rem 0.8rem;
  border: 1px solid rgba(51, 112, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-primary);
  font-size: 0.78rem;
  font-weight: 700;
}

.register-page__intro h1 {
  max-width: 10ch;
  margin: 1.2rem 0 0.9rem;
  font-size: clamp(2.6rem, 4vw, 4rem);
  line-height: 1.12;
  letter-spacing: -0.02em;
}

.register-page__intro p {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 1rem;
  line-height: 1.8;
}

.register-page__panel {
  display: grid;
  place-items: center;
  padding: 2rem;
}

.register-card {
  width: min(500px, 100%);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 42px rgba(31, 35, 41, 0.08);
}

.register-card__brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.register-card__logo {
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

.register-card__title {
  font-size: 1.4rem;
  font-weight: 700;
}

.register-card__subtitle {
  margin-top: 0.2rem;
  color: var(--color-text-muted);
}

.register-card__switch {
  display: inline-flex;
  margin-top: 1.2rem;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
}

.register-card__feedback {
  margin-top: 1rem;
}

.register-card__form {
  margin-top: 1.5rem;
}

.register-card__submit {
  width: 100%;
  height: 46px;
  margin-top: 0.5rem;
  font-weight: 700;
}

.register-card__footer {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  margin-top: 1.25rem;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.register-card__footer a {
  color: var(--color-primary);
  font-weight: 600;
}

@media (max-width: 980px) {
  .register-page {
    grid-template-columns: 1fr;
  }

  .register-page__intro {
    padding: 3rem 1.5rem 1rem;
  }

  .register-page__panel {
    padding: 1rem 1.5rem 2rem;
  }
}
</style>
