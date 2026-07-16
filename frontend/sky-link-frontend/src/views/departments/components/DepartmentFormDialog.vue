<script setup>
import { reactive, watch } from 'vue'
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'

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
    default: '保存',
  },
  saving: {
    type: Boolean,
    default: false,
  },
  leaderOptions: {
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
  departmentName: '',
  leaderId: '',
  description: '',
})

function syncForm() {
  localForm.departmentName = props.formData.departmentName ?? ''
  localForm.leaderId = props.formData.leaderId ?? ''
  localForm.description = props.formData.description ?? ''
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
    departmentName: localForm.departmentName,
    leaderId: localForm.leaderId,
    description: localForm.description,
  })
}
</script>

<template>
  <AppDialog :model-value="modelValue" :title="title" width="620px" @close="closeDialog">
    <el-form label-position="top" class="department-form">
      <el-form-item label="部门名称" required>
        <el-input
          v-model="localForm.departmentName"
          maxlength="50"
          show-word-limit
          placeholder="请输入部门名称"
        />
      </el-form-item>

      <el-form-item label="负责人">
        <el-select v-model="localForm.leaderId" clearable filterable placeholder="请选择负责人">
          <el-option
            v-for="user in leaderOptions"
            :key="user.userId"
            :label="`${user.nickname || user.username} · ${user.username}`"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="部门说明">
        <el-input
          v-model="localForm.description"
          type="textarea"
          :rows="4"
          maxlength="255"
          show-word-limit
          placeholder="请输入部门职责或说明"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <AppButton @click="closeDialog">取消</AppButton>
        <AppButton variant="primary" :loading="saving" @click="handleSubmit">
          {{ confirmText }}
        </AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.dialog-footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.department-form :deep(.el-select) {
  width: 100%;
}

.department-form :deep(.el-form-item__label) {
  color: var(--color-text);
  font-weight: 600;
}

.department-form :deep(.el-input__wrapper),
.department-form :deep(.el-select__wrapper) {
  min-height: 2.75rem;
  border-radius: var(--radius-sm);
}

.department-form :deep(.el-textarea__inner) {
  min-height: 7rem !important;
  border-radius: var(--radius-sm);
  line-height: 1.6;
}

@media (max-width: 560px) {
  .dialog-footer {
    flex-direction: column-reverse;
  }

  .dialog-footer .app-button {
    width: 100%;
  }
}
</style>
