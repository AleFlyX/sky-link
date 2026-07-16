<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppDialog from '../../../components/common/AppDialog.vue'

defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  department: {
    type: Object,
    default: null,
  },
  options: {
    type: Array,
    default: () => [],
  },
  selectedMemberIds: {
    type: Array,
    default: () => [],
  },
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'update:selected-member-ids', 'save'])
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="department ? `加入${department.departmentName}` : '加入成员'"
    width="640px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-position="top" class="department-form">
      <el-form-item label="选择成员" required>
        <el-select
          :model-value="selectedMemberIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="搜索并选择要加入的用户"
          @update:model-value="emit('update:selected-member-ids', $event)"
        >
          <el-option
            v-for="user in options"
            :key="user.userId"
            :label="`${user.nickname || user.username} · ${user.username}${user.departmentName ? ` · ${user.departmentName}` : ''}`"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>

      <el-alert
        v-if="!options.length"
        title="暂无可加入成员"
        type="info"
        show-icon
        :closable="false"
      />
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <AppButton @click="emit('update:modelValue', false)">取消</AppButton>
        <AppButton
          variant="primary"
          :loading="saving"
          @click="emit('save')"
        >
          加入部门
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
