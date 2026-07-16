<script setup>
import { reactive, ref, watch } from 'vue'
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'
import AppInput from '../../../components/common/AppInput.vue'
import { userStatusOptions } from '../../../constants/enums'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  confirmText: {
    type: String,
    default: '创建用户',
  },
  saving: {
    type: Boolean,
    default: false,
  },
  departmentOptions: {
    type: Array,
    default: () => [],
  },
  roleOptions: {
    type: Array,
    default: () => [],
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])
const userFormRef = ref()

const localForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  departmentId: '',
  status: 1,
  roleIds: [],
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为 3 至 50 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '用户名仅支持字母、数字和下划线', trigger: 'blur' },
  ],
  nickname: [{ max: 50, message: '昵称不能超过 50 个字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/,
      message: '密码至少 8 位，且需同时包含字母和数字',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
    { max: 100, message: '邮箱不能超过 100 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { max: 20, message: '手机号不能超过 20 个字符', trigger: 'blur' },
    {
      pattern: /^[0-9+()\-\s]{6,20}$/,
      message: '请输入正确的手机号格式',
      trigger: 'blur',
    },
  ],
}

function syncForm() {
  localForm.username = props.formData.username ?? ''
  localForm.password = props.formData.password ?? ''
  localForm.nickname = props.formData.nickname ?? ''
  localForm.email = props.formData.email ?? ''
  localForm.phone = props.formData.phone ?? ''
  localForm.departmentId = props.formData.departmentId ?? ''
  localForm.status = props.formData.status ?? 1
  localForm.roleIds = Array.isArray(props.formData.roleIds) ? [...props.formData.roleIds] : []
}

watch(
  () => [props.modelValue, props.formData],
  () => {
    if (props.modelValue) {
      syncForm()
    }
  },
  { deep: true, immediate: true },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function hasValidFormData() {
  const username = localForm.username.trim()
  const password = localForm.password
  const email = localForm.email.trim()
  const phone = localForm.phone.trim()

  return (
    /^[A-Za-z0-9_]{3,50}$/.test(username) &&
    /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(password) &&
    /^\S+@\S+\.\S+$/.test(email) &&
    email.length <= 100 &&
    /^[0-9+()\-\s]{6,20}$/.test(phone) &&
    localForm.nickname.length <= 50
  )
}

async function handleSubmit() {
  const isValid = await userFormRef.value?.validate().catch(() => false)
  if (!isValid || !hasValidFormData()) {
    return
  }

  emit('submit', {
    username: localForm.username,
    password: localForm.password,
    nickname: localForm.nickname,
    email: localForm.email,
    phone: localForm.phone,
    departmentId: localForm.departmentId,
    status: localForm.status,
    roleIds: [...localForm.roleIds],
  })
}
</script>

<template>
  <AppDialog :model-value="modelValue" :title="title" width="720px" @close="closeDialog">
    <el-form
      ref="userFormRef"
      :model="localForm"
      :rules="formRules"
      label-position="top"
      class="user-form-dialog"
    >
      <div class="user-form-dialog__grid">
        <el-form-item label="用户名" prop="username" required>
          <AppInput
            v-model="localForm.username"
            maxlength="50"
            show-word-limit
            placeholder="请输入用户名"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <AppInput
            v-model="localForm.nickname"
            maxlength="50"
            show-word-limit
            placeholder="不填则默认使用用户名"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" required>
          <AppInput
            v-model="localForm.password"
            type="password"
            show-password
            placeholder="至少 8 位，需包含字母和数字"
            autocomplete="new-password"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email" required>
          <AppInput
            v-model="localForm.email"
            placeholder="请输入邮箱"
            autocomplete="email"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone" required>
          <AppInput
            v-model="localForm.phone"
            placeholder="请输入手机号"
            autocomplete="tel"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="部门">
          <el-select
            v-model="localForm.departmentId"
            clearable
            filterable
            placeholder="请选择部门"
            :disabled="saving"
          >
            <el-option
              v-for="department in departmentOptions"
              :key="department.departmentId"
              :label="department.departmentName"
              :value="department.departmentId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="localForm.status" placeholder="请选择状态" :disabled="saving">
            <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="角色">
          <el-select
            v-model="localForm.roleIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择角色"
            :disabled="saving"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>
      </div>

      <p class="user-form-dialog__hint">不选择角色时，系统会自动分配默认 `ROLE_USER`。</p>
    </el-form>

    <template #footer>
      <div class="user-form-dialog__footer">
        <AppButton @click="closeDialog">取消</AppButton>
        <AppButton variant="primary" :loading="saving" @click="handleSubmit">
          {{ confirmText }}
        </AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.user-form-dialog {
  display: block;
}

.user-form-dialog__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 1rem;
}

.user-form-dialog__grid :deep(.el-select),
.user-form-dialog__grid :deep(.el-input) {
  width: 100%;
}

.user-form-dialog__hint {
  margin: 0.5rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.84rem;
  line-height: 1.6;
}

.user-form-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .user-form-dialog__grid {
    grid-template-columns: 1fr;
  }

  .user-form-dialog__footer {
    flex-direction: column-reverse;
  }

  .user-form-dialog__footer .app-button {
    width: 100%;
  }
}
</style>
