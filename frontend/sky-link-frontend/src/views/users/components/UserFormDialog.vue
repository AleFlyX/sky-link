<script setup>
import { reactive, watch } from 'vue'
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

function handleSubmit() {
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
    <el-form label-position="top" class="user-form-dialog">
      <div class="user-form-dialog__grid">
        <el-form-item label="用户名" required>
          <AppInput
            v-model="localForm.username"
            maxlength="50"
            show-word-limit
            placeholder="请输入用户名"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="昵称">
          <AppInput
            v-model="localForm.nickname"
            maxlength="50"
            show-word-limit
            placeholder="不填则默认使用用户名"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="密码" required>
          <AppInput
            v-model="localForm.password"
            type="password"
            show-password
            placeholder="至少 8 位，需包含字母和数字"
            autocomplete="new-password"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="邮箱" required>
          <AppInput
            v-model="localForm.email"
            placeholder="请输入邮箱"
            autocomplete="email"
            :disabled="saving"
          />
        </el-form-item>

        <el-form-item label="手机号" required>
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
