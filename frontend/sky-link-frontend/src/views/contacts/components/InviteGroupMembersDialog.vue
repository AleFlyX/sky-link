<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'
import AppInput from '../../../components/common/AppInput.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const userIdsText = ref('')

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      userIdsText.value = ''
    }
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  const userIds = [...new Set(
    userIdsText.value
      .split(/[\s,，]+/)
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isInteger(item) && item > 0),
  )]

  if (!userIds.length) {
    ElMessage.warning('请输入至少一个有效的用户 ID')
    return
  }

  emit('submit', userIds)
}
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    title="邀请成员"
    width="560px"
    @close="closeDialog"
  >
    <div class="invite-group-members-dialog">
      <p class="invite-group-members-dialog__hint">
        输入要邀请的用户 ID，多个 ID 可用逗号、空格或换行分隔。
      </p>

      <AppInput
        v-model="userIdsText"
        type="textarea"
        placeholder="例如：1002, 1003"
      />
    </div>

    <template #footer>
      <div class="invite-group-members-dialog__footer">
        <AppButton @click="closeDialog">取消</AppButton>
        <AppButton variant="primary" :loading="loading" @click="handleSubmit">发送邀请</AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.invite-group-members-dialog {
  display: grid;
  gap: 0.9rem;
}

.invite-group-members-dialog__hint {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.invite-group-members-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
