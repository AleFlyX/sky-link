<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppPagination from '../../../components/common/AppPagination.vue'
import FriendProfileCard from './FriendProfileCard.vue'

defineProps({
  friends: {
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
  total: {
    type: Number,
    default: 0,
  },
  pendingIncomingCount: {
    type: Number,
    default: 0,
  },
  page: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    default: 6,
  },
})

const emit = defineEmits([
  'update:page',
  'open-request-dialog',
  'open-chat',
])
</script>

<template>
  <AppCard title="好友通讯录" subtitle="查看好友并进入单聊">
    <div class="page-toolbar">
      <span class="friend-directory__hint">共 {{ total }} 位好友</span>
      <el-badge :value="pendingIncomingCount" :hidden="pendingIncomingCount === 0" :max="99">
        <AppButton @click="emit('open-request-dialog')">查看申请</AppButton>
      </el-badge>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <el-alert
      v-else-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="friend-directory__feedback"
    />
    <div v-else-if="friends.length" class="friend-grid">
      <FriendProfileCard
        v-for="friend in friends"
        :key="friend.id"
        :friend="friend"
        @open-chat="emit('open-chat', $event)"
      />
    </div>
    <div v-else class="friend-directory__empty">
      <strong>暂无匹配好友</strong>
      <span>请更换关键词，或先添加好友。</span>
    </div>

    <AppPagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="emit('update:page', $event)"
    />
  </AppCard>
</template>

<style scoped>
.friend-directory__hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.friend-directory__feedback {
  margin-top: 0.25rem;
}

.friend-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.friend-directory__empty span {
  color: var(--color-text-muted);
}

.friend-directory__empty {
  display: grid;
  gap: 0.4rem;
  padding: 1.5rem 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 18px;
  text-align: center;
}
</style>
