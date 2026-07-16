<script setup>
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppDataTable from '../../../components/common/AppDataTable.vue'
import AppInput from '../../../components/common/AppInput.vue'
import AppPagination from '../../../components/common/AppPagination.vue'

defineProps({
  columns: {
    type: Array,
    required: true,
  },
  groups: {
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
  groupsCount: {
    type: Number,
    default: 0,
  },
  keyword: {
    type: String,
    default: '',
  },
  page: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    default: 5,
  },
})

const emit = defineEmits([
  'update:keyword',
  'update:page',
  'search',
  'retry',
  'open-create-group',
  'open-manage',
  'open-chat',
])
</script>

<template>
  <AppCard title="群聊空间" subtitle="查看群聊并进入会话">
    <div class="page-toolbar">
      <span class="group-space__hint">共 {{ groupsCount }} 个群聊</span>
      <div class="group-space__toolbar-actions">
        <AppInput
          :model-value="keyword"
          clearable
          placeholder="搜索群聊名称 / 公告"
          @update:model-value="emit('update:keyword', $event)"
          @keyup.enter="emit('search')"
        />
        <AppButton variant="primary" @click="emit('open-create-group')">创建群聊</AppButton>
      </div>
    </div>

    <AppDataTable
      :columns="columns"
      :rows="groups"
      :loading="loading"
      :error="error"
      empty-text="暂无群聊"
      @retry="emit('retry')"
    >
      <template #actions="{ row }">
        <div class="group-space__actions">
          <AppButton size="small" @click="emit('open-manage', row)">管理</AppButton>
          <AppButton size="small" variant="primary" @click="emit('open-chat', row)"
            >发消息</AppButton
          >
        </div>
      </template>
    </AppDataTable>

    <AppPagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="emit('update:page', $event)"
    />
  </AppCard>
</template>

<style scoped>
.group-space__hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.group-space__toolbar-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.group-space__toolbar-actions .el-input {
  width: 260px;
}

.group-space__actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

@media (max-width: 900px) {
  .group-space__toolbar-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .group-space__toolbar-actions .el-input {
    width: 100%;
  }
}
</style>
