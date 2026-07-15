<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppDataTable from '../../../components/common/AppDataTable.vue'
import AppStatusTag from '../../../components/common/AppStatusTag.vue'

defineProps({
  requests: {
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
  actionLoading: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['retry', 'handle-request'])

const columns = [
  { key: 'name', label: '申请人' },
  { key: 'account', label: '账号' },
  { key: 'department', label: '部门' },
  { key: 'message', label: '附言' },
  { key: 'requestTime', label: '申请时间' },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'actions', label: '操作', width: '180px', align: 'center', slot: 'actions' },
]

const requestStatusMap = {
  pending: { label: '待处理', tone: 'warning' },
  accepted: { label: '已通过', tone: 'success' },
  rejected: { label: '已拒绝', tone: 'danger' },
}

function getRequestStatusMeta(status) {
  return requestStatusMap[status] || { label: status || '未知', tone: 'info' }
}
</script>

<template>
  <AppDataTable
    :columns="columns"
    :rows="requests"
    :loading="loading"
    :error="error"
    empty-text="暂无收到的好友申请"
    @retry="emit('retry')"
  >
    <template #status="{ value }">
      <AppStatusTag :label="getRequestStatusMeta(value).label" :tone="getRequestStatusMeta(value).tone" />
    </template>

    <template #actions="{ row }">
      <div class="friend-request-incoming-table__actions">
        <AppButton
          size="small"
          variant="primary"
          :loading="actionLoading === `accept-${row.requestId}`"
          @click="emit('handle-request', row.requestId, 'accept')"
        >
          同意
        </AppButton>
        <AppButton
          size="small"
          variant="danger"
          :loading="actionLoading === `reject-${row.requestId}`"
          @click="emit('handle-request', row.requestId, 'reject')"
        >
          拒绝
        </AppButton>
      </div>
    </template>
  </AppDataTable>
</template>

<style scoped>
.friend-request-incoming-table__actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}
</style>
