<script setup>
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppButton from '../../../components/common/AppButton.vue'

defineProps({
  keyword: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:keyword', 'search', 'reset', 'create'])
</script>

<template>
  <div class="department-toolbar">
    <div class="department-toolbar__filters">
      <el-input
        :model-value="keyword"
        placeholder="搜索部门名称 / 负责人 / 说明"
        clearable
        @update:model-value="emit('update:keyword', $event)"
        @keyup.enter="emit('search')"
      />
    </div>

    <div class="department-toolbar__actions">
      <AppButton variant="primary" :icon="Search" @click="emit('search')">查询</AppButton>
      <AppButton :icon="Refresh" @click="emit('reset')">重置</AppButton>
      <AppButton
        v-permission="'department:create'"
        variant="primary"
        :icon="Plus"
        @click="emit('create')"
        >新建部门</AppButton
      >
    </div>
  </div>
</template>

<style scoped>
.department-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.department-toolbar__filters {
  flex: 1;
  max-width: 34rem;
}

.department-toolbar__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 840px) {
  .department-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .department-toolbar__filters {
    max-width: none;
  }

  .department-toolbar__actions {
    flex-wrap: wrap;
  }

  .department-toolbar__actions .app-button {
    flex: 1;
  }
}
</style>
