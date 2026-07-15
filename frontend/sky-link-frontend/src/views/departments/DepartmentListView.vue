<script setup>
import { onMounted } from 'vue'
import { Delete, EditPen, UserFilled, View } from '@element-plus/icons-vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import DepartmentAddMembersDialog from './components/DepartmentAddMembersDialog.vue'
import DepartmentFormDialog from './components/DepartmentFormDialog.vue'
import DepartmentMembersDialog from './components/DepartmentMembersDialog.vue'
import DepartmentToolbar from './components/DepartmentToolbar.vue'
import { useDepartmentManagement } from './composables/useDepartmentManagement'

const {
  page,
  pageSize,
  loading,
  loadError,
  keyword,
  formVisible,
  formSaving,
  departmentForm,
  leaderOptions,
  formTitle,
  formConfirmText,
  membersVisible,
  membersLoading,
  membersError,
  membersPage,
  membersPageSize,
  membersTotal,
  memberRows,
  activeDepartment,
  addMembersVisible,
  addMembersSaving,
  selectedMemberIds,
  columns,
  filteredRows,
  pagedRows,
  availableMemberOptions,
  formatStatus,
  loadDepartments,
  refreshAll,
  handleSearch,
  handleReset,
  openCreateDialog,
  openEditDialog,
  saveDepartment,
  removeDepartment,
  openMembers,
  openAddMembers,
  loadMembers,
  saveMembers,
  removeMember,
} = useDepartmentManagement()

function updateSelectedMemberIds(value) {
  selectedMemberIds.value = value
}

onMounted(async () => {
  await refreshAll()
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      title="部门管理"
      subtitle="维护部门基础信息、负责人和成员归属，非空部门需先迁移成员后再删除。"
    >
      <DepartmentToolbar
        v-model:keyword="keyword"
        @search="handleSearch"
        @reset="handleReset"
        @create="openCreateDialog"
      />

      <AppDataTable
        row-key="departmentId"
        :columns="columns"
        :rows="pagedRows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无部门数据"
        @retry="loadDepartments"
      >
        <template #leaderName="{ value }">
          <span>{{ value || '未设置' }}</span>
        </template>

        <template #memberCount="{ row, value }">
          <AppButton size="small" :icon="UserFilled" @click="openMembers(row)">
            {{ value }} 人
          </AppButton>
        </template>

        <template #description="{ value }">
          <span>{{ value || '-' }}</span>
        </template>

        <template #actions="{ row }">
          <div class="row-actions">
            <AppButton size="small" :icon="View" @click="openMembers(row)">成员</AppButton>
            <AppButton v-permission="'department:update'" size="small" :icon="EditPen" @click="openEditDialog(row)">编辑</AppButton>
            <AppButton v-permission="'department:delete'" size="small" variant="danger" :icon="Delete" @click="removeDepartment(row)">
              删除
            </AppButton>
          </div>
        </template>
      </AppDataTable>

      <AppPagination
        v-model:page="page"
        :page-size="pageSize"
        :total="filteredRows.length"
      />
    </AppCard>

    <DepartmentFormDialog
      v-model="formVisible"
      :title="formTitle"
      :confirm-text="formConfirmText"
      :saving="formSaving"
      :leader-options="leaderOptions"
      :form-data="departmentForm"
      @submit="saveDepartment"
    />

    <DepartmentMembersDialog
      v-model="membersVisible"
      :department="activeDepartment"
      :member-rows="memberRows"
      :members-loading="membersLoading"
      :members-error="membersError"
      :members-total="membersTotal"
      :members-page="membersPage"
      :members-page-size="membersPageSize"
      :format-status="formatStatus"
      @retry="loadMembers"
      @page-change="loadMembers"
      @open-add-members="openAddMembers"
      @remove-member="removeMember"
    />

    <DepartmentAddMembersDialog
      v-model="addMembersVisible"
      :department="activeDepartment"
      :options="availableMemberOptions"
      :selected-member-ids="selectedMemberIds"
      :saving="addMembersSaving"
      @update:selected-member-ids="updateSelectedMemberIds"
      @save="saveMembers"
    />
  </div>
</template>

<style scoped>
.row-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
