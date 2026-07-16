<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  options: {
    type: Array,
    default: () => [],
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const selectedUserIds = ref([])

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      selectedUserIds.value = []
    }
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (!selectedUserIds.value.length) {
    ElMessage.warning('请选择至少一个用户')
    return
  }

  emit(
    'submit',
    selectedUserIds.value.map((item) => Number(item)),
  )
}
</script>

<template>
  <AppDialog :model-value="modelValue" title="邀请成员" width="560px" @close="closeDialog">
    <div class="invite-group-members-dialog">
      <p class="invite-group-members-dialog__hint">从用户列表中选择要邀请进群的成员。</p>

      <el-select
        v-model="selectedUserIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        :loading="loading"
        :disabled="loading || options.length === 0"
        placeholder="搜索并选择用户"
      >
        <el-option
          v-for="user in options"
          :key="user.value"
          :label="user.label"
          :value="user.value"
        />
      </el-select>

      <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" />

      <el-alert
        v-else-if="!loading && !options.length"
        title="暂无可邀请的用户"
        type="info"
        show-icon
        :closable="false"
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

.invite-group-members-dialog :deep(.el-select) {
  width: 100%;
}
</style>
