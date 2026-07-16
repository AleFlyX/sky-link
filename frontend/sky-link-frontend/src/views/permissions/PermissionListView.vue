<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Delete, EditPen, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppDialog from '../../components/common/AppDialog.vue'
import AppInput from '../../components/common/AppInput.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useConfirmDialog } from '../../composables/useConfirmDialog'
import {
  createPermission,
  deletePermission,
  getPermissionPage,
  updatePermission,
} from '../../api/permission'

const page = ref(1)
const pageSize = 10
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const loadError = ref('')

const filters = reactive({
  permissionName: '',
  permissionCode: '',
  permissionType: '',
})

const dialogVisible = ref(false)
const dialogSaving = ref(false)
const dialogMode = ref('create')
const permissionForm = reactive({
  permissionId: null,
  permissionName: '',
  permissionCode: '',
  permissionType: 3,
  sortNo: 0,
})

const { confirm } = useConfirmDialog()

const permissionTypeOptions = [
  { value: 1, label: '菜单权限', tone: 'primary' },
  { value: 2, label: '按钮权限', tone: 'warning' },
  { value: 3, label: '接口权限', tone: 'success' },
]

const permissionTypeMap = Object.fromEntries(permissionTypeOptions.map((item) => [item.value, item]))

const columns = [
  { key: 'permissionName', label: '权限名称' },
  { key: 'permissionCode', label: '权限编码' },
  { key: 'permissionType', label: '权限类型', slot: 'permissionType', width: '120px' },
  { key: 'sortNo', label: '排序号', width: '90px' },
  { key: 'actions', label: '操作', slot: 'actions', width: '180px', align: 'right' },
]

function unwrapData(response) {
  return response?.data ?? response ?? {}
}

function normalizeId(value) {
  return value === '' || value === null || value === undefined ? undefined : Number(value)
}

function getPermissionTypeMeta(value) {
  return permissionTypeMap[value] || { label: '未知', tone: 'default' }
}

async function loadPermissions(targetPage = page.value) {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getPermissionPage({
      page: targetPage,
      size: pageSize,
      permissionName: filters.permissionName.trim() || undefined,
      permissionCode: filters.permissionCode.trim() || undefined,
      permissionType: normalizeId(filters.permissionType),
    })
    const data = unwrapData(response)
    rows.value = data.records || []
    total.value = data.total || 0
    page.value = data.page || targetPage
  } catch (error) {
    loadError.value = error.message || '权限列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadPermissions(1)
}

function handleReset() {
  filters.permissionName = ''
  filters.permissionCode = ''
  filters.permissionType = ''
  page.value = 1
  loadPermissions(1)
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadPermissions(nextPage)
}

function resetPermissionForm() {
  permissionForm.permissionId = null
  permissionForm.permissionName = ''
  permissionForm.permissionCode = ''
  permissionForm.permissionType = 3
  permissionForm.sortNo = 0
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetPermissionForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogMode.value = 'edit'
  permissionForm.permissionId = row.permissionId
  permissionForm.permissionName = row.permissionName || ''
  permissionForm.permissionCode = row.permissionCode || ''
  permissionForm.permissionType = row.permissionType ?? 3
  permissionForm.sortNo = row.sortNo ?? 0
  dialogVisible.value = true
}

async function savePermission() {
  const payload = {
    permissionName: permissionForm.permissionName.trim(),
    permissionCode: permissionForm.permissionCode.trim(),
    permissionType: Number(permissionForm.permissionType ?? 3),
    sortNo: Number(permissionForm.sortNo ?? 0),
  }

  if (!payload.permissionName || !payload.permissionCode) {
    ElMessage.warning('请先填写完整的权限名称和权限编码')
    return
  }

  dialogSaving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createPermission(payload)
      ElMessage.success('权限已创建')
    } else {
      await updatePermission(permissionForm.permissionId, payload)
      ElMessage.success('权限已更新')
    }
    dialogVisible.value = false
    await loadPermissions(page.value)
  } catch (error) {
    ElMessage.error(error.message || '权限保存失败')
  } finally {
    dialogSaving.value = false
  }
}

async function removePermission(row) {
  try {
    await confirm(`确定要删除权限「${row.permissionName}」吗？`, '删除权限', {
      confirmText: '删除',
      cancelText: '取消',
      type: 'warning',
      confirmVariant: 'danger',
    })
  } catch {
    return
  }

  try {
    await deletePermission(row.permissionId)
    ElMessage.success('权限已删除')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadPermissions(page.value)
  } catch (error) {
    ElMessage.error(error.message || '删除权限失败')
  }
}

onMounted(async () => {
  await loadPermissions()
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      title="权限管理"
      subtitle="维护全局权限目录，角色页会基于这里的权限编码进行能力分配。"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <AppInput
            v-model="filters.permissionName"
            clearable
            placeholder="权限名称"
            @keyup.enter="handleSearch"
          />
          <AppInput
            v-model="filters.permissionCode"
            clearable
            placeholder="权限编码"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="filters.permissionType" clearable placeholder="权限类型">
            <el-option
              v-for="item in permissionTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="page-toolbar__actions">
          <AppButton
            v-permission="'permission:create'"
            variant="primary"
            :icon="Plus"
            @click="openCreateDialog"
          >
            新建权限
          </AppButton>
          <AppButton variant="primary" :icon="Search" @click="handleSearch">查询</AppButton>
          <AppButton :icon="Refresh" @click="handleReset">重置</AppButton>
        </div>
      </div>

      <AppDataTable
        row-key="permissionId"
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无权限数据"
        @retry="loadPermissions"
      >
        <template #permissionType="{ value }">
          <AppStatusTag
            :label="getPermissionTypeMeta(value).label"
            :tone="getPermissionTypeMeta(value).tone"
          />
        </template>

        <template #sortNo="{ value }">
          <span>{{ value ?? 0 }}</span>
        </template>

        <template #actions="{ row }">
          <div class="row-actions">
            <AppButton
              v-permission="'permission:update'"
              size="small"
              :icon="EditPen"
              @click="openEditDialog(row)"
            >
              编辑
            </AppButton>
            <AppButton
              v-permission="'permission:delete'"
              size="small"
              variant="danger"
              :icon="Delete"
              @click="removePermission(row)"
            >
              删除
            </AppButton>
          </div>
        </template>
      </AppDataTable>

      <AppPagination
        :page="page"
        :page-size="pageSize"
        :total="total"
        @update:page="handlePageChange"
      />
    </AppCard>

    <AppDialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建权限' : '编辑权限'"
      width="680px"
    >
      <div class="dialog-form">
        <label class="dialog-form__field">
          <span>权限名称</span>
          <AppInput v-model="permissionForm.permissionName" maxlength="50" placeholder="例如：角色列表" />
        </label>

        <label class="dialog-form__field">
          <span>权限编码</span>
          <AppInput v-model="permissionForm.permissionCode" maxlength="100" placeholder="例如：role:list" />
        </label>

        <label class="dialog-form__field">
          <span>权限类型</span>
          <el-select v-model="permissionForm.permissionType" class="dialog-form__select">
            <el-option
              v-for="item in permissionTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>

        <label class="dialog-form__field">
          <span>排序号</span>
          <el-input-number
            v-model="permissionForm.sortNo"
            :min="0"
            :max="9999"
            class="dialog-form__number"
            controls-position="right"
          />
        </label>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <AppButton :disabled="dialogSaving" @click="dialogVisible = false">取消</AppButton>
          <AppButton variant="primary" :loading="dialogSaving" @click="savePermission">
            {{ dialogMode === 'create' ? '创建权限' : '保存修改' }}
          </AppButton>
        </div>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.page-toolbar__filters {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.page-toolbar__actions,
.row-actions,
.dialog-footer {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.page-toolbar__actions {
  flex: 0 0 auto;
}

.row-actions,
.dialog-footer {
  justify-content: flex-end;
}

.dialog-form {
  display: grid;
  gap: 1rem;
}

.dialog-form__field {
  display: grid;
  gap: 0.45rem;
}

.dialog-form__field span {
  color: var(--color-text-muted);
}

.dialog-form__select,
.dialog-form__number {
  width: 100%;
}

.dialog-form__number :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--color-border) inset;
  border-radius: 12px;
}

.dialog-form__number :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(51, 112, 255, 0.42) inset;
}

.dialog-form__number :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 3px rgba(51, 112, 255, 0.12);
}

@media (max-width: 900px) {
  .page-toolbar {
    flex-direction: column;
  }

  .page-toolbar__filters {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .page-toolbar__actions {
    width: 100%;
  }

  .page-toolbar__actions :deep(.app-button) {
    flex: 1;
  }
}
</style>
