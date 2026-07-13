<script setup>
import { computed, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { taskStatusMap, taskStatusOptions } from '../../constants/enums'
import { tasks } from '../../mock/workspace'

const keyword = ref('')
const status = ref('')
const page = ref(1)
const pageSize = 5
const dialogVisible = ref(false)

const columns = [
  { key: 'title', label: '任务标题' },
  { key: 'assignee', label: '负责人' },
  { key: 'priority', label: '优先级' },
  { key: 'status', label: '任务状态', slot: 'status' },
  { key: 'dueDate', label: '截止日期' },
]

const filteredRows = computed(() =>
  tasks.filter((item) => {
    const matchKeyword = [item.title, item.assignee, item.priority].some((text) =>
      text.toLowerCase().includes(keyword.value.toLowerCase()),
    )
    const matchStatus = !status.value || item.status === status.value
    return matchKeyword && matchStatus
  }),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRows.value.slice(start, start + pageSize)
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      variant="default"
      title="任务管理"
      subtitle="任务状态枚举、筛选和新增弹窗已统一，可承接成员 B 的任务列表联调"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <el-input v-model="keyword" placeholder="搜索任务 / 负责人 / 优先级" clearable />
          <el-select v-model="status" placeholder="筛选任务状态" clearable>
            <el-option
              v-for="item in taskStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <AppButton variant="primary" @click="dialogVisible = true">新建任务</AppButton>
      </div>

      <AppDataTable :columns="columns" :rows="pagedRows" empty-text="暂无任务数据">
        <template #status="{ value }">
          <AppStatusTag :label="taskStatusMap[value].label" :tone="taskStatusMap[value].tone" />
        </template>
      </AppDataTable>

      <AppPagination v-model:page="page" :page-size="pageSize" :total="filteredRows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="新建任务"
      confirm-text="保存任务"
      :fields="[
        { key: 'title', label: '任务标题' },
        { key: 'assignee', label: '负责人' },
        { key: 'status', label: '任务状态', type: 'select', options: taskStatusOptions },
      ]"
      :form-data="{ title: '', assignee: '', status: 'todo' }"
      @submit="() => {}"
    />
  </div>
</template>
