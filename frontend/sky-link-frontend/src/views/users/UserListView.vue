<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, EditPen, Refresh, Search, SwitchButton, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppDialog from '../../components/common/AppDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { getDepartments } from '../../api/department'
import { getRoles } from '../../api/role'
import {
  assignUserRoles,
  deleteUser,
  getUser,
  getUsers,
  updateUserStatus,
} from '../../api/user'
import { userStatusMap, userStatusOptions } from '../../constants/enums'

const page = ref(1)
const pageSize = 10
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const loadError = ref('')

const filters = reactive({
  username: '',
  nickname: '',
  departmentId: '',
  status: '',
})

const departmentOptions = ref([])
const roleOptions = ref([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailUser = ref(null)
const detailRoleIds = ref([])
const detailRoleSaving = ref(false)

const columns = [
  { key: 'userId', label: '用户ID', width: '88px' },
  { key: 'username', label: '用户名' },
  { key: 'nickname', label: '昵称' },
  { key: 'departmentName', label: '部门' },
  { key: 'email', label: '邮箱' },
  { key: 'phone', label: '手机号' },
  { key: 'status', label: '状态', slot: 'status', width: '96px' },
  { key: 'createTime', label: '创建时间', width: '180px' },
  { key: 'actions', label: '操作', slot: 'actions', width: '220px', align: 'right' },
]

const selectedDepartmentLabel = computed(() => {
  if (filters.departmentId === '' || filters.departmentId === null || filters.departmentId === undefined) {
    return ''
  }
  const department = departmentOptions.value.find((item) => item.departmentId === Number(filters.departmentId))
  return department?.departmentName || ''
})

function unwrapData(response) {
  return response?.data ?? response ?? {}
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function normalizeId(value) {
  return value === '' || value === null || value === undefined ? undefined : Number(value)
}

function statusMeta(value) {
  return userStatusMap[value] || { label: '未知', tone: 'default' }
}

async function loadDepartments() {
  const response = await getDepartments()
  const data = unwrapData(response)
  departmentOptions.value = Array.isArray(data) ? data : data.records || []
}

async function loadRoles() {
  const response = await getRoles({ page: 1, size: 200 })
  const data = unwrapData(response)
  roleOptions.value = data.records || []
}

async function loadUsers(targetPage = page.value) {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getUsers({
      page: targetPage,
      size: pageSize,
      username: filters.username.trim() || undefined,
      nickname: filters.nickname.trim() || undefined,
      departmentId: normalizeId(filters.departmentId),
      status: normalizeId(filters.status),
    })
    const data = unwrapData(response)
    rows.value = data.records || []
    total.value = data.total || 0
    page.value = data.page || targetPage
  } catch (error) {
    loadError.value = error.message || '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.allSettled([
    loadDepartments(),
    loadRoles(),
    loadUsers(page.value),
  ])
}

function handleSearch() {
  page.value = 1
  loadUsers(1)
}

function handleReset() {
  filters.username = ''
  filters.nickname = ''
  filters.departmentId = ''
  filters.status = ''
  page.value = 1
  loadUsers(1)
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadUsers(nextPage)
}

async function openDetail(userId) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailUser.value = null
  detailRoleIds.value = []

  try {
    const response = await getUser(userId)
    const data = unwrapData(response)
    detailUser.value = data
    detailRoleIds.value = Array.isArray(data.roles) ? data.roles.map((role) => role.roleId) : []
  } catch (error) {
    detailError.value = error.message || '用户详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

async function saveRoles() {
  if (!detailUser.value) {
    return
  }

  detailRoleSaving.value = true
  try {
    const response = await assignUserRoles(detailUser.value.userId, detailRoleIds.value)
    const data = unwrapData(response)
    if (Array.isArray(data)) {
      detailUser.value = { ...detailUser.value, roles: data }
    }
    ElMessage.success('角色已更新')
    await loadUsers(page.value)
    await openDetail(detailUser.value.userId)
  } catch (error) {
    ElMessage.error(error.message || '角色更新失败')
  } finally {
    detailRoleSaving.value = false
  }
}

async function changeStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionLabel = nextStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(
      `确定要${actionLabel}用户「${row.nickname || row.username}」吗？`,
      `${actionLabel}用户`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await updateUserStatus(row.userId, nextStatus)
    ElMessage.success(`已${actionLabel}`)
    await loadUsers(page.value)
    if (detailVisible.value && detailUser.value?.userId === row.userId) {
      await openDetail(row.userId)
    }
  } catch (error) {
    ElMessage.error(error.message || `${actionLabel}失败`)
  }
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.nickname || row.username}」吗？`,
      '删除用户',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await deleteUser(row.userId)
    ElMessage.success('用户已删除')
    if (detailVisible.value && detailUser.value?.userId === row.userId) {
      detailVisible.value = false
    }
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadUsers(page.value)
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function getRoleLabel(role) {
  return `${role.roleName}${role.roleCode ? ` · ${role.roleCode}` : ''}`
}

onMounted(async () => {
  await refreshAll()
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      title="用户管理"
      subtitle="按用户名、昵称、部门和状态筛选用户，支持查看详情、启停用、删除和角色分配。"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <el-input
            v-model="filters.username"
            placeholder="用户名"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="filters.nickname"
            placeholder="昵称"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-select v-model="filters.departmentId" placeholder="部门" clearable>
            <el-option
              v-for="department in departmentOptions"
              :key="department.departmentId"
              :label="department.departmentName"
              :value="department.departmentId"
            />
          </el-select>
          <el-select v-model="filters.status" placeholder="状态" clearable>
            <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="page-toolbar__actions">
          <AppButton variant="primary" :icon="Search" @click="handleSearch">查询</AppButton>
          <AppButton :icon="Refresh" @click="handleReset">重置</AppButton>
        </div>
      </div>

      <el-alert
        v-if="selectedDepartmentLabel"
        :title="`当前部门筛选：${selectedDepartmentLabel}`"
        type="info"
        show-icon
        :closable="true"
        class="page-feedback"
      />

      <AppDataTable
        row-key="userId"
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无用户数据"
        @retry="loadUsers"
      >
        <template #status="{ value }">
          <AppStatusTag
            :label="statusMeta(value).label"
            :tone="statusMeta(value).tone"
          />
        </template>

        <template #createTime="{ value }">
          <span>{{ formatDateTime(value) }}</span>
        </template>

        <template #actions="{ row }">
          <div class="row-actions">
            <AppButton size="small" :icon="View" @click="openDetail(row.userId)">详情</AppButton>
            <AppButton
              size="small"
              :icon="SwitchButton"
              :variant="row.status === 1 ? 'warning' : 'success'"
              @click="changeStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </AppButton>
            <AppButton size="small" variant="danger" :icon="Delete" @click="removeUser(row)">
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
      v-model="detailVisible"
      title="用户详情"
      width="860px"
    >
      <el-skeleton v-if="detailLoading" :rows="6" animated />

      <el-alert
        v-else-if="detailError"
        :title="detailError"
        type="error"
        show-icon
        :closable="false"
      />

      <div v-else-if="detailUser" class="detail-panel">
        <div class="detail-hero">
          <div class="detail-hero__avatar">
            {{ (detailUser.nickname || detailUser.username || '?').slice(0, 1) }}
          </div>
          <div class="detail-hero__copy">
            <div class="detail-hero__title-row">
              <h3>{{ detailUser.nickname || detailUser.username }}</h3>
              <AppStatusTag
                :label="statusMeta(detailUser.status).label"
                :tone="statusMeta(detailUser.status).tone"
              />
            </div>
            <p>{{ detailUser.username }}</p>
            <div class="detail-hero__meta">
              <span>ID {{ detailUser.userId }}</span>
              <span>{{ detailUser.departmentName || '未分配部门' }}</span>
              <span>创建于 {{ formatDateTime(detailUser.createTime) }}</span>
            </div>
          </div>
        </div>

        <div class="detail-grid">
          <section class="detail-section">
            <h4>基础信息</h4>
            <dl class="detail-list">
              <div>
                <dt>用户名</dt>
                <dd>{{ detailUser.username || '-' }}</dd>
              </div>
              <div>
                <dt>昵称</dt>
                <dd>{{ detailUser.nickname || '-' }}</dd>
              </div>
              <div>
                <dt>部门</dt>
                <dd>{{ detailUser.departmentName || '-' }}</dd>
              </div>
              <div>
                <dt>状态</dt>
                <dd>{{ statusMeta(detailUser.status).label }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(detailUser.createTime) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDateTime(detailUser.updateTime) }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h4>角色管理</h4>
            <el-select
              v-model="detailRoleIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择角色"
              class="role-select"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.roleId"
                :label="getRoleLabel(role)"
                :value="role.roleId"
              />
            </el-select>
            <div class="detail-section__footer">
              <AppButton
                variant="primary"
                :icon="EditPen"
                :loading="detailRoleSaving"
                @click="saveRoles"
              >
                保存角色
              </AppButton>
            </div>
            <div class="role-tags">
              <AppStatusTag
                v-for="role in detailUser.roles || []"
                :key="role.roleId"
                :label="getRoleLabel(role)"
                tone="primary"
              />
              <span v-if="!detailUser.roles || !detailUser.roles.length" class="muted-text">
                暂无角色
              </span>
            </div>
          </section>
        </div>
      </div>
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.page-toolbar__actions {
  display: flex;
  gap: 0.75rem;
  flex: 0 0 auto;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.detail-panel {
  display: grid;
  gap: 1.25rem;
}

.detail-hero {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.detail-hero__avatar {
  display: grid;
  place-items: center;
  width: 4rem;
  height: 4rem;
  border-radius: 1rem;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 1.5rem;
  font-weight: 800;
  flex: 0 0 auto;
}

.detail-hero__copy {
  min-width: 0;
  flex: 1;
}

.detail-hero__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.detail-hero__copy h3 {
  margin: 0;
  font-size: 1.25rem;
}

.detail-hero__copy p {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

.detail-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.detail-hero__meta span {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 1rem;
}

.detail-section {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.detail-section h4 {
  margin: 0;
  font-size: 1rem;
}

.detail-list {
  display: grid;
  gap: 0.7rem;
  margin: 0;
}

.detail-list div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.detail-list dt {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.detail-list dd {
  margin: 0;
  text-align: right;
  color: var(--color-text);
}

.role-select {
  width: 100%;
}

.detail-section__footer {
  display: flex;
  justify-content: flex-end;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.muted-text {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

@media (max-width: 960px) {
  .page-toolbar,
  .detail-hero,
  .detail-list div {
    flex-direction: column;
    align-items: stretch;
  }

  .page-toolbar__filters,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-list dd {
    text-align: left;
  }
}

@media (max-width: 720px) {
  .page-toolbar__filters {
    grid-template-columns: 1fr;
  }

  .page-toolbar__actions {
    width: 100%;
  }

  .page-toolbar__actions .app-button {
    flex: 1;
  }
}
</style>
