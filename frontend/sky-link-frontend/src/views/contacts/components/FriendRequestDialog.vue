<script setup>
import AppDialog from '../../../components/common/AppDialog.vue'
import FriendRequestIncomingTable from './FriendRequestIncomingTable.vue'
import FriendRequestOutgoingTable from './FriendRequestOutgoingTable.vue'
import FriendRequestOverviewCards from './FriendRequestOverviewCards.vue'

defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  activeTab: {
    type: String,
    default: 'incoming',
  },
  incomingRequests: {
    type: Array,
    default: () => [],
  },
  outgoingRequests: {
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

const emit = defineEmits([
  'update:modelValue',
  'update:activeTab',
  'retry',
  'handle-request',
])

function closeDialog() {
  emit('update:modelValue', false)
}

function forwardHandleRequest(requestId, action) {
  emit('handle-request', requestId, action)
}
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    title="好友申请"
    width="980px"
    @close="closeDialog"
  >
    <div class="request-dialog">
      <FriendRequestOverviewCards
        :incoming-count="incomingRequests.length"
        :outgoing-count="outgoingRequests.length"
        :pending-count="incomingRequests.filter((item) => item.status === 'pending').length"
      />

      <el-tabs
        :model-value="activeTab"
        class="request-dialog__tabs"
        @update:model-value="emit('update:activeTab', $event)"
      >
        <el-tab-pane :label="`收到的申请 (${incomingRequests.length})`" name="incoming">
          <FriendRequestIncomingTable
            :requests="incomingRequests"
            :loading="loading"
            :error="error"
            :action-loading="actionLoading"
            @retry="emit('retry')"
            @handle-request="forwardHandleRequest"
          />
        </el-tab-pane>

        <el-tab-pane :label="`我发出的申请 (${outgoingRequests.length})`" name="outgoing">
          <FriendRequestOutgoingTable
            :requests="outgoingRequests"
            :loading="loading"
            :error="error"
            @retry="emit('retry')"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </AppDialog>
</template>

<style scoped>
.request-dialog {
  display: grid;
  gap: 1rem;
}

.request-dialog__tabs {
  min-width: 0;
}
</style>
