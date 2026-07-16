<script setup>
import { Refresh, Search } from '@element-plus/icons-vue'
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppInput from '../../../components/common/AppInput.vue'
import AppDataTable from '../../../components/common/AppDataTable.vue'
import AppFormDialog from '../../../components/common/AppFormDialog.vue'
import AppPagination from '../../../components/common/AppPagination.vue'
import AppStatusTag from '../../../components/common/AppStatusTag.vue'
import { getTaskStatusMeta, taskStatusOptions } from '../../../constants/enums'
import { useTaskList } from '../composables/useTaskList'

const {
  columns,
  demoData,
  dialogVisible,
  getNextStatusAction,
  handlePageChange,
  handleReset,
  handleSearch,
  handleStatusUpdate,
  handleSubmit,
  keyword,
  loadData,
  loadError,
  loading,
  page,
  pageSize,
  rows,
  status,
  taskCreationDisabled,
  taskCreationNotice,
  taskFormFields,
  taskFormInitialData,
  total,
  updatingTaskId,
} = useTaskList()
</script>

<template>
  <div class="page-shell">
    <AppCard
      variant="default"
      title="任务管理"
      subtitle="任务只能分配给当前部门成员，状态枚举、筛选和新增弹窗已统一"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <AppInput
            v-model="keyword"
            placeholder="搜索任务"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-select v-model="status" placeholder="筛选任务状态" clearable @change="handleSearch">
            <el-option
              v-for="item in taskStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <AppButton
          v-permission="'task:create'"
          variant="primary"
          :disabled="taskCreationDisabled"
          @click="dialogVisible = true"
        >
          新建任务
        </AppButton>
        <AppButton variant="primary" :icon="Search" @click="handleSearch">查询</AppButton>
        <AppButton :icon="Refresh" @click="handleReset">重置</AppButton>
      </div>

      <el-alert
        v-if="demoData"
        title="当前为演示数据模式，任务创建与筛选可完整演示"
        type="info"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <el-alert
        v-if="taskCreationNotice"
        :title="taskCreationNotice"
        type="warning"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <AppDataTable
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无任务数据"
        @retry="loadData"
      >
        <template #status="{ value }">
          <AppStatusTag
            :label="getTaskStatusMeta(value).label"
            :tone="getTaskStatusMeta(value).tone"
          />
        </template>
        <template #actions="{ row }">
          <AppButton
            v-if="getNextStatusAction(row)"
            v-permission="'task:status:update'"
            size="small"
            variant="secondary"
            :loading="updatingTaskId === row.id"
            @click="handleStatusUpdate(row)"
          >
            {{ getNextStatusAction(row).label }}
          </AppButton>
        </template>
      </AppDataTable>

      <AppPagination
        :page="page"
        :page-size="pageSize"
        :total="total"
        @update:page="handlePageChange"
      />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="新建任务"
      confirm-text="保存任务"
      :fields="taskFormFields"
      :form-data="taskFormInitialData"
      @submit="handleSubmit"
    />
  </div>
</template>
