<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, EditPen, Lock, Plus, Refresh, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppDialog from '../../components/common/AppDialog.vue'
import AppInput from '../../components/common/AppInput.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { useConfirmDialog } from '../../composables/useConfirmDialog'
import { getPermissions } from '../../api/permission'
import {
  createRole,
  deleteRole,
  getRoles,
  updateRole,
  updateRolePermissions,
} from '../../api/role'

const page = ref(1)
const pageSize = 10
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const permissionOptions = ref([])
const permissionLoading = ref(false)
const permissionLoadError = ref('')

const filters = reactive({
  roleName: '',
  roleCode: '',
  status: '',
})

const dialogVisible = ref(false)
const dialogSaving = ref(false)
const dialogMode = ref('create')
const roleForm = reactive({
  roleId: null,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1,
})

const permissionDialogVisible = ref(false)
const permissionDialogSaving = ref(false)
const activeRole = ref(null)
const selectedPermissionCodes = ref([])
const { confirm } = useConfirmDialog()

const roleStatusOptions = [
  { value: 1, label: '启用', tone: 'success' },
  { value: 0, label: '禁用', tone: 'danger' },
]

const roleStatusMap = Object.fromEntries(roleStatusOptions.map((item) => [item.value, item]))
const permissionTypeOptions = [
  { value: 1, label: '菜单权限' },
  { value: 2, label: '按钮权限' },
  { value: 3, label: '接口权限' },
]

const columns = [
  { key: 'roleName', label: '角色名称' },
  { key: 'roleCode', label: '角色编码' },
  { key: 'status', label: '状态', slot: 'status', width: '96px' },
  { key: 'description', label: '角色说明' },
  { key: 'permissions', label: '权限范围', slot: 'permissions', width: '300px' },
  { key: 'actions', label: '操作', slot: 'actions', width: '280px', align: 'right' },
]

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建角色' : '编辑角色'))
const dialogConfirmText = computed(() => (dialogMode.value === 'create' ? '创建角色' : '保存修改'))

const groupedPermissionOptions = computed(() =>
  permissionTypeOptions.map((group) => ({
    ...group,
    items: permissionOptions.value.filter((item) => Number(item.permissionType) === group.value),
  })).filter((group) => group.items.length),
)

function unwrapData(response) {
  return response?.data ?? response ?? {}
}

function normalizeId(value) {
  return value === '' || value === null || value === undefined ? undefined : Number(value)
}

function normalizeRole(row) {
  return {
    ...row,
    roleId: row.roleId,
    permissions: Array.isArray(row.permissions) ? row.permissions : [],
  }
}

function getRoleStatusMeta(value) {
  return roleStatusMap[value] || { label: '未知', tone: 'default' }
}

function rolePermissionSummary(role) {
  const permissions = role.permissions || []
  if (!permissions.length) {
    return '未分配权限'
  }
  return permissions.slice(0, 3).map((item) => item.permissionCode).join('、')
}

async function loadRoles(targetPage = page.value) {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getRoles({
      page: targetPage,
      size: pageSize,
      roleName: filters.roleName.trim() || undefined,
      roleCode: filters.roleCode.trim() || undefined,
      status: normalizeId(filters.status),
    })
    const data = unwrapData(response)
    rows.value = (data.records || []).map(normalizeRole)
    total.value = data.total || 0
    page.value = data.page || targetPage
  } catch (error) {
    loadError.value = error.message || '角色列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  permissionLoading.value = true
  permissionLoadError.value = ''
  try {
    const response = await getPermissions()
    permissionOptions.value = unwrapData(response)
  } catch (error) {
    permissionOptions.value = []
    permissionLoadError.value = error.message || '权限目录加载失败'
  } finally {
    permissionLoading.value = false
  }
}

async function refreshAll() {
  await loadRoles(page.value)
}

function handleSearch() {
  page.value = 1
  loadRoles(1)
}

function handleReset() {
  filters.roleName = ''
  filters.roleCode = ''
  filters.status = ''
  page.value = 1
  loadRoles(1)
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadRoles(nextPage)
}

function resetRoleForm() {
  roleForm.roleId = null
  roleForm.roleName = ''
  roleForm.roleCode = ''
  roleForm.description = ''
  roleForm.status = 1
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetRoleForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogMode.value = 'edit'
  roleForm.roleId = row.roleId
  roleForm.roleName = row.roleName || ''
  roleForm.roleCode = row.roleCode || ''
  roleForm.description = row.description || ''
  roleForm.status = row.status ?? 1
  dialogVisible.value = true
}

async function saveRole() {
  const payload = {
    roleName: roleForm.roleName.trim(),
    roleCode: roleForm.roleCode.trim(),
    description: roleForm.description.trim() || undefined,
  }

  if (!payload.roleName || !payload.roleCode) {
    ElMessage.warning('请先填写完整的角色名称和角色编码')
    return
  }

  dialogSaving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRole(payload)
      ElMessage.success('角色已创建')
    } else {
      await updateRole(roleForm.roleId, { ...payload, status: Number(roleForm.status ?? 1) })
      ElMessage.success('角色已更新')
    }
    dialogVisible.value = false
    await loadRoles(page.value)
  } catch (error) {
    ElMessage.error(error.message || '角色保存失败')
  } finally {
    dialogSaving.value = false
  }
}

async function removeRole(row) {
  try {
    await confirm(`确定要删除角色「${row.roleName}」吗？`, '删除角色', {
      confirmText: '删除',
      cancelText: '取消',
      type: 'warning',
      confirmVariant: 'danger',
    })
  } catch {
    return
  }

  try {
    await deleteRole(row.roleId)
    ElMessage.success('角色已删除')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadRoles(page.value)
  } catch (error) {
    ElMessage.error(error.message || '删除角色失败')
  }
}

async function openPermissionDialog(role) {
  activeRole.value = role
  selectedPermissionCodes.value = Array.isArray(role.permissions)
    ? role.permissions.map((item) => item.permissionCode)
    : []
  permissionDialogVisible.value = true
  if (!permissionOptions.value.length && !permissionLoading.value) {
    await loadPermissionOptions()
  }
}

async function saveRolePermissions() {
  if (!activeRole.value) {
    return
  }

  permissionDialogSaving.value = true
  try {
    await updateRolePermissions(activeRole.value.roleId, selectedPermissionCodes.value)
    ElMessage.success('角色权限已更新')
    permissionDialogVisible.value = false
    await loadRoles(page.value)
  } catch (error) {
    ElMessage.error(error.message || '权限保存失败')
  } finally {
    permissionDialogSaving.value = false
  }
}

onMounted(async () => {
  await refreshAll()
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      title="角色管理"
      subtitle="集中维护角色元数据，并为每个角色分配可访问的系统权限。"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <AppInput
            v-model="filters.roleName"
            clearable
            placeholder="角色名称"
            @keyup.enter="handleSearch"
          />
          <AppInput
            v-model="filters.roleCode"
            clearable
            placeholder="角色编码"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="filters.status" clearable placeholder="角色状态">
            <el-option
              v-for="item in roleStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="page-toolbar__actions">
          <AppButton
            v-permission="'role:create'"
            variant="primary"
            :icon="Plus"
            @click="openCreateDialog"
          >
            新建角色
          </AppButton>
          <AppButton variant="primary" :icon="Setting" @click="handleSearch">查询</AppButton>
          <AppButton :icon="Refresh" @click="handleReset">重置</AppButton>
        </div>
      </div>

      <AppDataTable
        row-key="roleId"
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无角色数据"
        @retry="loadRoles"
      >
        <template #status="{ value }">
          <AppStatusTag
            :label="getRoleStatusMeta(value).label"
            :tone="getRoleStatusMeta(value).tone"
          />
        </template>

        <template #description="{ value }">
          <span>{{ value || '-' }}</span>
        </template>

        <template #permissions="{ row }">
          <div class="permission-summary">
            <span class="permission-summary__count">{{ row.permissions?.length || 0 }} 项</span>
            <span class="permission-summary__codes">{{ rolePermissionSummary(row) }}</span>
          </div>
        </template>

        <template #actions="{ row }">
          <div class="row-actions">
            <AppButton
              v-permission="'role:permission:set'"
              size="small"
              :icon="Lock"
              @click="openPermissionDialog(row)"
            >
              分配权限
            </AppButton>
            <AppButton
              v-permission="'role:update'"
              size="small"
              :icon="EditPen"
              @click="openEditDialog(row)"
            >
              编辑
            </AppButton>
            <AppButton
              v-permission="'role:delete'"
              size="small"
              variant="danger"
              :icon="Delete"
              @click="removeRole(row)"
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

    <AppDialog v-model="dialogVisible" :title="dialogTitle" width="680px">
      <div class="dialog-form">
        <label class="dialog-form__field">
          <span>角色名称</span>
          <AppInput v-model="roleForm.roleName" maxlength="50" placeholder="例如：项目主管" />
        </label>

        <label class="dialog-form__field">
          <span>角色编码</span>
          <AppInput v-model="roleForm.roleCode" maxlength="50" placeholder="例如：ROLE_PROJECT_LEADER" />
        </label>

        <label class="dialog-form__field">
          <span>角色说明</span>
          <AppInput v-model="roleForm.description" type="textarea" maxlength="255" show-word-limit placeholder="说明该角色的职责边界" />
        </label>

        <label v-if="dialogMode === 'edit'" class="dialog-form__field">
          <span>角色状态</span>
          <el-select v-model="roleForm.status" class="dialog-form__select">
            <el-option
              v-for="item in roleStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <AppButton :disabled="dialogSaving" @click="dialogVisible = false">取消</AppButton>
          <AppButton variant="primary" :loading="dialogSaving" @click="saveRole">
            {{ dialogConfirmText }}
          </AppButton>
        </div>
      </template>
    </AppDialog>

    <AppDialog
      v-model="permissionDialogVisible"
      :title="`分配权限 · ${activeRole?.roleName || ''}`"
      width="860px"
    >
      <div class="permission-panel">
        <el-alert
          v-if="permissionLoadError"
          :title="permissionLoadError"
          type="error"
          show-icon
          :closable="false"
        />

        <el-skeleton v-else-if="permissionLoading" :rows="6" animated />

        <template v-else>
          <p class="permission-panel__hint">
            为当前角色勾选可访问的能力点。保存时将按权限编码整体覆盖。
          </p>

          <el-checkbox-group v-model="selectedPermissionCodes" class="permission-groups">
            <section
              v-for="group in groupedPermissionOptions"
              :key="group.value"
              class="permission-group"
            >
              <header class="permission-group__header">
                <h4>{{ group.label }}</h4>
                <span>{{ group.items.length }} 项</span>
              </header>

              <div class="permission-group__list">
                <label
                  v-for="item in group.items"
                  :key="item.permissionCode"
                  class="permission-item"
                >
                  <el-checkbox :label="item.permissionCode">
                    <span class="permission-item__name">{{ item.permissionName }}</span>
                    <span class="permission-item__code">{{ item.permissionCode }}</span>
                  </el-checkbox>
                </label>
              </div>
            </section>
          </el-checkbox-group>
        </template>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <AppButton :disabled="permissionDialogSaving" @click="permissionDialogVisible = false">取消</AppButton>
          <AppButton
            variant="primary"
            :loading="permissionDialogSaving"
            @click="saveRolePermissions"
          >
            保存权限
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

.row-actions {
  justify-content: flex-end;
}

.permission-summary {
  display: grid;
  gap: 0.2rem;
}

.permission-summary__count {
  font-weight: 700;
}

.permission-summary__codes {
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.dialog-form {
  display: grid;
  gap: 1rem;
}

.dialog-form__field {
  display: grid;
  gap: 0.45rem;
}

.dialog-form__field span,
.permission-panel__hint,
.permission-group__header span,
.permission-item__code {
  color: var(--color-text-muted);
}

.dialog-form__select {
  width: 100%;
}

.dialog-footer {
  justify-content: flex-end;
}

.permission-panel {
  display: grid;
  gap: 1rem;
}

.permission-groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.permission-group {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.permission-group__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}

.permission-group__header h4 {
  margin: 0;
  font-size: 1rem;
}

.permission-group__list {
  display: grid;
  gap: 0.85rem;
}

.permission-item {
  display: block;
  min-width: 0;
}

.permission-item :deep(.el-checkbox) {
  align-items: flex-start;
}

.permission-item__name,
.permission-item__code {
  display: block;
}

.permission-item__name {
  color: var(--color-text);
  font-weight: 600;
}

.permission-item__code {
  margin-top: 0.12rem;
  font-size: 0.82rem;
  line-height: 1.5;
  word-break: break-all;
}

@media (max-width: 1080px) {
  .permission-groups {
    grid-template-columns: 1fr;
  }
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
