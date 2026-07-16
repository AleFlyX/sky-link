<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  options: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const form = reactive({
  friendUserId: '',
  message: '',
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }

    form.friendUserId = ''
    form.message = ''
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (!form.friendUserId) {
    ElMessage.warning('请选择要添加的用户')
    return
  }

  emit('submit', {
    friendUserId: Number(form.friendUserId),
    message: form.message.trim(),
  })
}
</script>

<template>
  <AppDialog :model-value="modelValue" title="添加好友" width="560px" @close="closeDialog">
    <el-form label-position="top" class="add-friend-form">
      <el-form-item label="选择用户" required>
        <el-select
          v-model="form.friendUserId"
          filterable
          clearable
          :loading="loading"
          :disabled="loading || options.length === 0"
          placeholder="搜索并选择要添加的用户"
        >
          <el-option
            v-for="user in options"
            :key="user.value"
            :label="user.label"
            :value="user.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="申请附言">
        <el-input
          v-model="form.message"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="介绍一下你自己"
        />
      </el-form-item>

      <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" />

      <el-alert
        v-else-if="!loading && !options.length"
        title="暂无可添加的用户"
        type="info"
        show-icon
        :closable="false"
      />
    </el-form>

    <template #footer>
      <div class="add-friend-form__footer">
        <AppButton @click="closeDialog">取消</AppButton>
        <AppButton variant="primary" :loading="loading" @click="handleSubmit">发送申请</AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.add-friend-form :deep(.el-select) {
  width: 100%;
}

.add-friend-form :deep(.el-form-item__label) {
  color: var(--color-text);
  font-weight: 600;
}

.add-friend-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 560px) {
  .add-friend-form__footer {
    flex-direction: column-reverse;
  }

  .add-friend-form__footer .app-button {
    width: 100%;
  }
}
</style>
