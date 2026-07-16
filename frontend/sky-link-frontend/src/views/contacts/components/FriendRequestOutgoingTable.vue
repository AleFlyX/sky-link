<script setup>
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
})

const emit = defineEmits(['retry'])

const columns = [
  { key: 'name', label: '目标用户' },
  { key: 'account', label: '账号' },
  { key: 'department', label: '部门' },
  { key: 'message', label: '附言' },
  { key: 'requestTime', label: '申请时间' },
  { key: 'status', label: '状态', slot: 'status' },
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
    empty-text="暂无发出的好友申请"
    @retry="emit('retry')"
  >
    <template #status="{ value }">
      <AppStatusTag
        :label="getRequestStatusMeta(value).label"
        :tone="getRequestStatusMeta(value).tone"
      />
    </template>
  </AppDataTable>
</template>
