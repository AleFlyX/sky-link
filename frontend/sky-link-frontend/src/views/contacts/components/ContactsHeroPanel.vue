<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppInput from '../../../components/common/AppInput.vue'
import ContactsStatsBoard from './ContactsStatsBoard.vue'

defineProps({
  keyword: {
    type: String,
    default: '',
  },
  friendsCount: {
    type: Number,
    default: 0,
  },
  groupsCount: {
    type: Number,
    default: 0,
  },
  incomingRequestsCount: {
    type: Number,
    default: 0,
  },
  pendingIncomingCount: {
    type: Number,
    default: 0,
  },
  demoData: {
    type: Boolean,
    default: false,
  },
  loadError: {
    type: String,
    default: '',
  },
  requestDialogVisible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:keyword',
  'search',
  'refresh',
  'open-add-friend',
  'open-friend-requests',
  'open-create-group',
])
</script>

<template>
  <AppCard class="contacts-hero" variant="hero" padding="lg">
    <div class="contacts-hero__body">
      <div class="contacts-hero__top">
        <ContactsStatsBoard
          :friends-count="friendsCount"
          :groups-count="groupsCount"
          :incoming-requests-count="incomingRequestsCount"
          :pending-incoming-count="pendingIncomingCount"
        />
      </div>

      <AppCard variant="ghost" padding="sm" body-class="contacts-hero__toolbar">
        <div class="contacts-hero__search">
          <AppInput
            :model-value="keyword"
            clearable
            placeholder="搜索好友姓名 / 账号 / 部门"
            @update:model-value="emit('update:keyword', $event)"
            @keyup.enter="emit('search')"
          />
          <AppButton @click="emit('refresh')">刷新数据</AppButton>
        </div>

        <div class="contacts-hero__actions">
          <AppButton variant="primary" @click="emit('open-add-friend')">添加好友</AppButton>
          <el-badge :value="pendingIncomingCount" :hidden="pendingIncomingCount === 0" :max="99">
            <AppButton :active="requestDialogVisible" @click="emit('open-friend-requests')">
              好友申请
            </AppButton>
          </el-badge>
          <AppButton variant="primary" @click="emit('open-create-group')">创建群聊</AppButton>
        </div>
      </AppCard>

      <el-alert
        v-if="demoData"
        title="当前为演示数据模式，好友申请和群聊数据会即时反馈"
        type="info"
        show-icon
        :closable="false"
        class="contacts-hero__feedback"
      />
      <el-alert
        v-else-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        class="contacts-hero__feedback"
      />
    </div>
  </AppCard>
</template>

<style scoped>
.contacts-hero__body {
  display: grid;
  gap: 1.25rem;
}

.contacts-hero__top {
  width: 100%;
}
.contacts-hero__summary {
  max-width: 32rem;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.98rem;
  line-height: 1.7;
}

.contacts-hero__stats-wrap {
  min-width: 0;
}

:deep(.contacts-hero__toolbar) {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(420px, 0.95fr);
  gap: 1rem;
  align-items: center;
}

.contacts-hero__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.contacts-hero__search .app-input {
  min-width: 0;
}

.contacts-hero__actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.contacts-hero__actions :deep(.el-badge__content) {
  z-index: 1;
}

.contacts-hero__actions :deep(.el-badge) {
  display: block;
  width: 100%;
}

.contacts-hero__actions :deep(.app-button) {
  width: 100%;
}

.contacts-hero__feedback {
  margin-top: -0.25rem;
}

@media (max-width: 1100px) {
  .contacts-hero__top,
  :deep(.contacts-hero__toolbar) {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .contacts-hero__search {
    grid-template-columns: 1fr;
  }

  .contacts-hero__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .contacts-hero__summary {
    font-size: 0.95rem;
  }
}
</style>
