<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDialog from '../../components/common/AppDialog.vue'
import AppInput from '../../components/common/AppInput.vue'
import { updateCurrentUser } from '../../api/user'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const systemIdentity = computed(() => userStore.user.roleLabel || '未分配')
const profileDepartment = computed(() => userStore.user.department || '未加入部门')
const editVisible = ref(false)
const saving = ref(false)
const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
})

function openEditDialog() {
  profileForm.nickname = userStore.user.name || ''
  profileForm.email = userStore.user.email || ''
  profileForm.phone = userStore.user.phone || ''
  editVisible.value = true
}

async function saveProfile() {
  const payload = {
    nickname: profileForm.nickname.trim(),
    email: profileForm.email.trim(),
    phone: profileForm.phone.trim(),
  }

  if (!payload.nickname || !payload.email || !payload.phone) {
    ElMessage.warning('昵称、邮箱和手机号不能为空')
    return
  }

  saving.value = true
  try {
    const response = await updateCurrentUser(payload)
    const profile = response?.data ?? response ?? {}
    userStore.patchUser({
      ...profile,
      name: profile.nickname || profile.username || payload.nickname,
    })
    editVisible.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
    ElMessage.error(error.message || '个人资料更新失败')
  } finally {
    saving.value = false
  }
}

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
        <AppButton
          v-permission="'user:me:update'"
          class="profile-hero__edit"
          :icon="EditPen"
          @click="openEditDialog"
        >
          编辑资料
        </AppButton>
      </div>
    </AppCard>

    <AppCard title="账号信息" class="profile-account-card">
      <div class="info-list">
        <div>
          <strong>账号邮箱</strong><span>{{ userStore.user.email }}</span>
        </div>
        <div>
          <strong>联系电话</strong><span>{{ userStore.user.phone }}</span>
        </div>
        <div>
          <strong>所属部门</strong><span>{{ profileDepartment }}</span>
        </div>
        <div>
          <strong>系统身份</strong><span>{{ systemIdentity }}</span>
        </div>
      </div>
    </AppCard>

    <AppDialog v-model="editVisible" title="编辑个人资料" width="560px">
      <div class="profile-form">
        <label class="profile-form__field">
          <span>昵称</span>
          <AppInput v-model="profileForm.nickname" maxlength="50" placeholder="请输入昵称" />
        </label>
        <label class="profile-form__field">
          <span>邮箱</span>
          <AppInput v-model="profileForm.email" maxlength="100" placeholder="请输入邮箱" />
        </label>
        <label class="profile-form__field">
          <span>手机号</span>
          <AppInput v-model="profileForm.phone" maxlength="30" placeholder="请输入手机号" />
        </label>
      </div>

      <template #footer>
        <div class="profile-form__footer">
          <AppButton :disabled="saving" @click="editVisible = false">取消</AppButton>
          <AppButton variant="primary" :loading="saving" @click="saveProfile">保存修改</AppButton>
        </div>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.profile-hero__edit {
  margin-left: auto;
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

.profile-form {
  display: grid;
  gap: 1rem;
}

.profile-form__field {
  display: grid;
  gap: 0.45rem;
}

.profile-form__field span {
  color: var(--color-text-muted);
}

.profile-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 720px) {
  .profile-hero,
  .info-list div {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-hero__edit {
    width: 100%;
    margin-left: 0;
  }

  .info-list span {
    text-align: left;
  }
}
</style>
