<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { getDocumentPermissions, removeDocumentPermission, setDocumentPermission } from '../../api/document'
import { getDocuments, isDemoMode, saveDocument } from '../../api/workspace'

const router = useRouter(); const keyword = ref(''); const page = ref(1); const pageSize = 6
const rows = ref([]); const loading = ref(false); const loadError = ref(''); const demoData = ref(isDemoMode())
const createDialog = ref(false); const permissionDialog = ref(false); const selectedDocument = ref(null)
const grants = ref([]); const grantLoading = ref(false)
const grantForm = reactive({ userId: '', permissionType: 'read' })
const columns = [
  { key: 'title', label: '文档标题' }, { key: 'author', label: '作者' },
  { key: 'status', label: '可见范围' }, { key: 'updatedAt', label: '最近更新' }, { key: 'actions', label: '操作' },
]
const unwrap = (payload) => payload?.data ?? payload
const normalize = (item) => ({ ...item, id: item.id ?? item.documentId, author: item.author ?? item.creatorName,
  updatedAt: item.updatedAt ?? item.updateTime })

async function loadData() {
  loading.value = true; loadError.value = ''
  try {
    const result = await getDocuments({ page: 1, size: 100, keyword: keyword.value })
    rows.value = (result.data.records || []).map(normalize); demoData.value = result.source === 'demo'
    if (result.degraded) loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
  } catch (error) { loadError.value = error.message }
  finally { loading.value = false }
}
async function handleCreate(form) {
  const result = await saveDocument(form); createDialog.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '文档接口不可用，已保存到演示数据' : '文档已创建')
  await loadData()
}
function openDocument(row) { router.push(`/app/documents/${row.id}/edit`) }
async function openPermissions(row) {
  selectedDocument.value = row; permissionDialog.value = true; grantLoading.value = true
  try { const result = unwrap(await getDocumentPermissions(row.id)); grants.value = result?.users || [] }
  catch (error) { ElMessage.error(error.message) }
  finally { grantLoading.value = false }
}
async function saveGrant() {
  const userId = Number(grantForm.userId)
  if (!Number.isInteger(userId) || userId < 1) return ElMessage.warning('请输入有效用户 ID')
  await setDocumentPermission(selectedDocument.value.id, userId, grantForm.permissionType)
  grantForm.userId = ''; await openPermissions(selectedDocument.value); ElMessage.success('权限已保存')
}
async function removeGrant(userId) {
  await removeDocumentPermission(selectedDocument.value.id, userId)
  await openPermissions(selectedDocument.value); ElMessage.success('权限已移除')
}
onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <AppCard title="在线文档" subtitle="创建文档、管理协作者并进入实时协同编辑">
      <div class="page-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索文档标题 / 作者" @keyup.enter="loadData" />
        <AppButton variant="primary" @click="createDialog = true">新建文档</AppButton>
      </div>
      <el-alert v-if="demoData" title="演示数据不支持真实协同连接" type="info" show-icon :closable="false" class="page-feedback" />
      <AppDataTable :columns="columns" :rows="rows" :loading="loading" :error="loadError" empty-text="暂无文档" @retry="loadData">
        <template #title="{ value, row }"><button type="button" class="document-title" @click="openDocument(row)">{{ value }}</button></template>
        <template #status="{ value }"><AppStatusTag :label="value === 'team' ? '部门可见' : value === 'archived' ? '已归档' : '仅自己可见'" :tone="value === 'archived' ? 'info' : 'primary'" /></template>
        <template #actions="{ row }"><div class="row-actions"><AppButton variant="primary" @click="openDocument(row)">编辑</AppButton><AppButton v-if="row.permission === 'manage'" variant="secondary" @click="openPermissions(row)">协作者</AppButton></div></template>
      </AppDataTable>
      <AppPagination v-model:page="page" :page-size="pageSize" :total="rows.length" />
    </AppCard>
    <AppFormDialog v-model="createDialog" title="新建文档" confirm-text="创建文档"
      :fields="[{ key: 'title', label: '文档标题', required: true }, { key: 'status', label: '可见范围', type: 'select', options: [{ value: 'private', label: '仅自己可见' }, { value: 'team', label: '部门可见' }] }, { key: 'content', label: '初始 Markdown', type: 'textarea' }]"
      :form-data="{ title: '', status: 'private', content: '' }" @submit="handleCreate" />
    <el-dialog v-model="permissionDialog" :title="`协作者 · ${selectedDocument?.title || ''}`" width="min(620px, 94vw)">
      <div class="grant-form"><el-input v-model="grantForm.userId" placeholder="用户 ID" /><el-select v-model="grantForm.permissionType"><el-option label="只读" value="read" /><el-option label="编辑" value="edit" /><el-option label="管理" value="manage" /></el-select><AppButton variant="primary" @click="saveGrant">保存授权</AppButton></div>
      <el-table v-loading="grantLoading" :data="grants" empty-text="暂无直接授权"><el-table-column prop="user.nickname" label="用户"><template #default="{ row }">{{ row.user?.nickname || row.user?.username || row.userId }}</template></el-table-column><el-table-column prop="permissionType" label="权限" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" @click="removeGrant(row.userId)">移除</el-button></template></el-table-column></el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.document-title{padding:0;border:0;background:transparent;color:var(--color-primary);font:inherit;font-weight:600;cursor:pointer}.row-actions,.grant-form{display:flex;align-items:center;gap:.75rem}.grant-form{margin-bottom:1rem}.grant-form .el-input{flex:1}.grant-form .el-select{width:130px}@media(max-width:640px){.grant-form{align-items:stretch;flex-direction:column}.grant-form .el-select{width:100%}}
</style>
