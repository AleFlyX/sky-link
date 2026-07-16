<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppInput from '../../components/common/AppInput.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import {
  getDocumentPermissions,
  removeDocumentPermission,
  setDocumentPermission,
} from '../../api/document'
import { getDocuments, getUsers, isDemoMode, saveDocument } from '../../api/workspace'
import { useUserStore } from '../../stores/user'
import { visibilityOptions as statusOptions } from './constant/enum.js'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const page = ref(1)
const pageSize = 6
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())
const createDialog = ref(false)
const permissionDialog = ref(false)
const selectedDocument = ref(null)
const statusDialog = ref(false)
const statusTarget = ref(null)
const statusSaving = ref(false)
const statusForm = reactive({ status: 'private' })
const grants = ref([])
const grantLoading = ref(false)
const userOptionsLoading = ref(false)
const userOptionsError = ref('')
const selectableUsers = ref([])
const grantForm = reactive({ userId: '', permissionType: 'read' })
const adminRoleCodes = new Set(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])

const statusLabels = Object.fromEntries(statusOptions.map((option) => [option.value, option.label]))
const currentUserId = computed(() => {
  const value = userStore.user?.id ?? userStore.user?.userId
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : null
})
const collaboratorOptions = computed(() => {
  const grantedUserIds = new Set(
    grants.value.map((item) => Number(item.userId)).filter(Number.isFinite),
  )
  const creatorId = Number(selectedDocument.value?.creatorId)

  return selectableUsers.value
    .filter((user) => Number(user.userId) !== currentUserId.value)
    .filter((user) => !grantedUserIds.has(Number(user.userId)))
    .filter((user) => !Number.isFinite(creatorId) || Number(user.userId) !== creatorId)
    .map((user) => ({
      value: user.userId,
      label: `${user.nickname || user.username || `用户#${user.userId}`}${user.departmentName ? ` · ${user.departmentName}` : ''}`,
    }))
})

const columns = [
  { key: 'title', label: '文档标题' },
  { key: 'author', label: '作者' },
  { key: 'status', label: '可见范围' },
  { key: 'updatedAt', label: '最近更新' },
  { key: 'actions', label: '操作' },
]
const unwrap = (payload) => payload?.data ?? payload
const normalize = (item) => ({
  ...item,
  id: item.id ?? item.documentId,
  author: item.author ?? item.creatorName,
  updatedAt: item.updatedAt ?? item.updateTime,
})

function normalizeUser(item) {
  return {
    userId: item?.userId ?? item?.id,
    username: item?.username ?? item?.account ?? '',
    nickname: item?.nickname ?? item?.name ?? '',
    departmentName: item?.departmentName ?? item?.department ?? '',
  }
}

function hasAdminRole() {
  return (Array.isArray(userStore.user?.roles) ? userStore.user.roles : []).some((role) => {
    if (!role) return false
    const code = String(
      role.roleCode || role.code || role.name || role.roleName || role.label || role,
    )
      .trim()
      .toUpperCase()
    return adminRoleCodes.has(code)
  })
}
function canManageRow(row) {
  return (
    row?.permission === 'manage' ||
    Number(row?.creatorId) === Number(userStore.user?.id) ||
    hasAdminRole()
  )
}

async function loadData(targetPage = page.value) {
  loading.value = true
  loadError.value = ''
  try {
    const result = await getDocuments({ page: targetPage, size: pageSize, keyword: keyword.value })
    const data = result.data || {}
    rows.value = (data.records || []).map(normalize)
    total.value = data.total || 0
    page.value = data.page || targetPage
    demoData.value = result.source === 'demo'
    if (result.degraded) loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
  } catch (error) {
    rows.value = []
    total.value = 0
    loadError.value = error.message
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData(1)
}

function handleReset() {
  keyword.value = ''
  page.value = 1
  loadData(1)
}

function handlePageChange(nextPage) {
  page.value = nextPage
  loadData(nextPage)
}

async function loadSelectableUsers() {
  userOptionsLoading.value = true
  userOptionsError.value = ''
  try {
    const result = await getUsers({ page: 1, size: 500 })
    selectableUsers.value = (result?.data?.records || []).map(normalizeUser)
  } catch (error) {
    selectableUsers.value = []
    userOptionsError.value = error.message || '用户列表加载失败'
  } finally {
    userOptionsLoading.value = false
  }
}

async function handleCreate(form) {
  const result = await saveDocument(form)
  createDialog.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](
    result.degraded ? '文档接口不可用，已保存到演示数据' : '文档已创建',
  )
  await loadData()
}

function openDocument(row) {
  router.push(`/app/documents/${row.id}/edit`)
}

async function openPermissions(row) {
  selectedDocument.value = row
  permissionDialog.value = true
  grantLoading.value = true
  grantForm.userId = ''
  grantForm.permissionType = 'read'

  try {
    await loadSelectableUsers()
    const result = unwrap(await getDocumentPermissions(row.id))
    grants.value = result?.users || []
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    grantLoading.value = false
  }
}

function openStatusDialog(row) {
  statusTarget.value = row
  statusForm.status = row.status || 'private'
  statusDialog.value = true
}

async function saveGrant() {
  const userId = Number(grantForm.userId)
  if (!Number.isInteger(userId) || userId < 1) {
    ElMessage.warning('请选择有效用户')
    return
  }

  await setDocumentPermission(selectedDocument.value.id, userId, grantForm.permissionType)
  grantForm.userId = ''
  await openPermissions(selectedDocument.value)
  ElMessage.success('权限已保存')
}

async function removeGrant(userId) {
  await removeDocumentPermission(selectedDocument.value.id, userId)
  await openPermissions(selectedDocument.value)
  ElMessage.success('权限已移除')
}

async function saveStatus() {
  if (!statusTarget.value) return
  const nextStatus = statusForm.status
  if (nextStatus === statusTarget.value.status) {
    statusDialog.value = false
    return
  }
  statusSaving.value = true
  try {
    const result = await saveDocument({ status: nextStatus }, statusTarget.value.id)
    statusTarget.value.status = nextStatus
    statusDialog.value = false
    ElMessage[result.degraded ? 'warning' : 'success'](
      result.degraded ? '文档接口不可用，已保存到演示数据' : '可见范围已保存',
    )
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusSaving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <AppCard title="在线文档" subtitle="查看文档列表并管理协作者">
      <div class="page-toolbar">
        <AppInput
          v-model="keyword"
          clearable
          placeholder="搜索文档标题 / 作者"
          @keyup.enter="handleSearch"
        />
        <AppButton variant="primary" @click="handleSearch">查询</AppButton>
        <AppButton @click="handleReset">重置</AppButton>
        <AppButton v-permission="'document:create'" variant="primary" @click="createDialog = true"
          >新建文档</AppButton
        >
      </div>
      <el-alert
        v-if="demoData"
        title="演示数据不支持真实协同连接"
        type="info"
        show-icon
        :closable="false"
        class="page-feedback"
      />
      <AppDataTable
        :columns="columns"
        :rows="rows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无文档"
        @retry="loadData"
      >
        <template #title="{ value, row }"
          ><button type="button" class="document-title" @click="openDocument(row)">
            {{ value }}
          </button></template
        >
        <template #status="{ value }"
          ><AppStatusTag
            :label="statusLabels[value] || '仅自己可见'"
            :tone="value === 'archived' ? 'info' : 'primary'"
        /></template>
        <template #actions="{ row }">
          <div class="row-actions">
            <AppButton v-permission="'document:update'" variant="primary" @click="openDocument(row)"
              >进入</AppButton
            >
            <AppButton
              v-if="canManageRow(row)"
              v-permission="'document:permission:list'"
              variant="secondary"
              @click="openPermissions(row)"
              >协作者</AppButton
            >
            <AppButton
              v-if="canManageRow(row)"
              v-permission="'document:update'"
              variant="secondary"
              @click="openStatusDialog(row)"
              >状态管理</AppButton
            >
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
    <AppFormDialog
      v-model="createDialog"
      title="新建文档"
      confirm-text="创建文档"
      :fields="[
        { key: 'title', label: '文档标题', required: true },
        {
          key: 'status',
          label: '可见范围',
          type: 'select',
          options: [
            { value: 'private', label: '仅自己可见' },
            { value: 'team', label: '部门可见' },
          ],
        },
        { key: 'content', label: '初始 Markdown', type: 'textarea' },
      ]"
      :form-data="{ title: '', status: 'private', content: '' }"
      @submit="handleCreate"
    />
    <el-dialog
      v-model="permissionDialog"
      :title="`协作者 · ${selectedDocument?.title || ''}`"
      width="min(620px, 94vw)"
    >
      <div class="grant-form">
        <el-select
          v-model="grantForm.userId"
          filterable
          clearable
          :loading="userOptionsLoading"
          :disabled="userOptionsLoading || collaboratorOptions.length === 0"
          placeholder="搜索并选择用户"
        >
          <el-option
            v-for="user in collaboratorOptions"
            :key="user.value"
            :label="user.label"
            :value="user.value"
          />
        </el-select>
        <el-select v-model="grantForm.permissionType">
          <el-option label="只读" value="read" />
          <el-option label="编辑" value="edit" />
          <el-option label="管理" value="manage" />
        </el-select>
        <AppButton
          v-permission="'document:permission:user:set'"
          variant="primary"
          @click="saveGrant"
          >保存授权</AppButton
        >
      </div>

      <el-alert
        v-if="userOptionsError"
        :title="userOptionsError"
        type="warning"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <el-alert
        v-else-if="!userOptionsLoading && !collaboratorOptions.length"
        title="暂无可授权的用户"
        type="info"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <el-table v-loading="grantLoading" :data="grants" empty-text="暂无直接授权">
        <el-table-column prop="user.nickname" label="用户">
          <template #default="{ row }">
            {{ row.user?.nickname || row.user?.username || row.userId }}
          </template>
        </el-table-column>
        <el-table-column prop="permissionType" label="权限" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-permission="'document:permission:user:delete'"
              link
              type="danger"
              @click="removeGrant(row.userId)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog v-model="statusDialog" title="状态管理" width="min(520px, 92vw)">
      <div class="status-form">
        <p class="status-form__hint">状态变化会影响列表展示和编辑页权限，归档后文档会转为只读。</p>
        <el-select v-model="statusForm.status" class="status-form__select" :disabled="statusSaving">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
      <template #footer>
        <div class="status-form__footer">
          <AppButton :disabled="statusSaving" @click="statusDialog = false">取消</AppButton>
          <AppButton variant="primary" :disabled="statusSaving" @click="saveStatus">保存</AppButton>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.document-title {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}
.row-actions,
.grant-form {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.grant-form {
  margin-bottom: 1rem;
}
.grant-form .app-input {
  flex: 1;
}
.grant-form .el-select {
  width: 130px;
}
.status-form {
  display: grid;
  gap: 1rem;
}
.status-form__hint {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}
.status-form__select {
  width: 100%;
}
.status-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
@media (max-width: 640px) {
  .grant-form {
    align-items: stretch;
    flex-direction: column;
  }
  .grant-form .el-select {
    width: 100%;
  }
}
.page-toolbar {
  display: flex;
  flex-direction: row;
  justify-content: start;
}
</style>
