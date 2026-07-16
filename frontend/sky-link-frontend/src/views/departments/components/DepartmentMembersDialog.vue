<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppDataTable from '../../../components/common/AppDataTable.vue'
import AppDialog from '../../../components/common/AppDialog.vue'
import AppPagination from '../../../components/common/AppPagination.vue'
import AppStatusTag from '../../../components/common/AppStatusTag.vue'

defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  department: {
    type: Object,
    default: null,
  },
  memberRows: {
    type: Array,
    default: () => [],
  },
  membersLoading: {
    type: Boolean,
    default: false,
  },
  membersError: {
    type: String,
    default: '',
  },
  membersTotal: {
    type: Number,
    default: 0,
  },
  membersPage: {
    type: Number,
    default: 1,
  },
  membersPageSize: {
    type: Number,
    default: 8,
  },
  formatStatus: {
    type: Function,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue', 'page-change', 'retry', 'open-add-members', 'remove-member'])

const memberColumns = [
  { key: 'userId', label: '用户ID', width: '88px' },
  { key: 'username', label: '用户名' },
  { key: 'nickname', label: '昵称' },
  { key: 'email', label: '邮箱' },
  { key: 'phone', label: '手机号' },
  { key: 'status', label: '状态', slot: 'status', width: '92px' },
  { key: 'actions', label: '操作', slot: 'actions', width: '96px', align: 'right' },
]
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="department ? `${department.departmentName}成员` : '部门成员'"
    width="900px"
    @close="emit('update:modelValue', false)"
  >
    <div class="members-toolbar">
      <div>
        <strong>{{ department?.departmentName || '部门' }}</strong>
        <span>当前 {{ membersTotal }} 人</span>
      </div>
      <AppButton v-permission="'department:members:add'" variant="primary" @click="emit('open-add-members')">
        加入成员
      </AppButton>
    </div>

    <AppDataTable
      row-key="userId"
      :columns="memberColumns"
      :rows="memberRows"
      :loading="membersLoading"
      :error="membersError"
      empty-text="暂无部门成员"
      @retry="emit('retry')"
    >
      <template #status="{ value }">
        <AppStatusTag
          :label="formatStatus(value).label"
          :tone="formatStatus(value).tone"
        />
      </template>

      <template #actions="{ row }">
        <AppButton v-permission="'department:members:remove'" size="small" variant="danger" @click="emit('remove-member', row)">
          移出
        </AppButton>
      </template>
    </AppDataTable>

    <AppPagination
      :page="membersPage"
      :page-size="membersPageSize"
      :total="membersTotal"
      @update:page="emit('page-change', $event)"
    />
  </AppDialog>
</template>

<style scoped>
.members-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.members-toolbar div {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  min-width: 0;
}

.members-toolbar strong {
  color: var(--color-text);
  font-size: 1rem;
}

.members-toolbar span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

@media (max-width: 840px) {
  .members-toolbar,
  .members-toolbar div {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
