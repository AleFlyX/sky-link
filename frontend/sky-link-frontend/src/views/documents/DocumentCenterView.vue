<script setup>
import { onMounted, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { getDocument, getDocuments, isDemoMode, saveDocument } from '../../api/workspace'

const keyword = ref('')
const page = ref(1)
const pageSize = 6
const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())
const createDialog = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const columns = [
  { key: 'title', label: '文档标题' },
  { key: 'author', label: '作者' },
  { key: 'status', label: '可见范围' },
  { key: 'updatedAt', label: '最近更新' },
]

async function loadData() {
  loading.value = true
  loadError.value = ''
  const result = await getDocuments({ page: 1, size: 100, keyword: keyword.value })
  rows.value = result.data.records || []
  demoData.value = result.source === 'demo'
  if (result.degraded) loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
  loading.value = false
}

async function openDocument(row) {
  detailLoading.value = true
  detailVisible.value = true
  const result = await getDocument(row.id)
  detail.value = result.data
  demoData.value = result.source === 'demo' || demoData.value
  detailLoading.value = false
}

async function handleCreate(form) {
  const result = await saveDocument(form)
  createDialog.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '文档接口不可用，已保存到演示数据' : '文档已创建')
  await loadData()
}

async function handleSave() {
  if (!detail.value) return
  const result = await saveDocument({ title: detail.value.title, content: detail.value.content, status: detail.value.status }, detail.value.id)
  detail.value = result.data
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '文档接口不可用，已保存演示版本' : '文档已保存')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <AppCard title="在线文档" subtitle="浏览文档列表、查看详情并直接保存普通文本内容">
      <div class="page-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索文档标题 / 作者" @keyup.enter="loadData" />
        <AppButton variant="primary" @click="createDialog = true">新建文档</AppButton>
      </div>

      <el-alert v-if="demoData" title="当前为演示数据模式，文档列表和详情均可操作" type="info" show-icon :closable="false" class="page-feedback" />
      <AppDataTable :columns="columns" :rows="rows" :loading="loading" :error="loadError" empty-text="暂无文档" @retry="loadData">
        <template #title="{ value, row }">
          <button type="button" class="document-title" @click="openDocument(row)">{{ value }}</button>
        </template>
        <template #status="{ value }">
          <AppStatusTag :label="value === 'team' ? '团队可见' : '仅自己可见'" :tone="value === 'team' ? 'primary' : 'info'" />
        </template>
      </AppDataTable>
      <AppPagination v-model:page="page" :page-size="pageSize" :total="rows.length" />
    </AppCard>

    <AppFormDialog
      v-model="createDialog"
      title="新建文档"
      confirm-text="创建文档"
      :fields="[
        { key: 'title', label: '文档标题', required: true },
        { key: 'status', label: '可见范围', type: 'select', options: [{ value: 'private', label: '仅自己可见' }, { value: 'team', label: '团队可见' }] },
        { key: 'content', label: '文档内容', type: 'textarea' },
      ]"
      :form-data="{ title: '', status: 'private', content: '' }"
      @submit="handleCreate"
    />

    <el-drawer v-model="detailVisible" title="文档详情" size="min(680px, 100%)">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="document-detail">
        <el-form label-position="top">
          <el-form-item label="文档标题">
            <el-input v-model="detail.title" />
          </el-form-item>
          <el-form-item label="文档内容">
            <el-input v-model="detail.content" type="textarea" :rows="18" resize="vertical" />
          </el-form-item>
        </el-form>
        <div class="document-detail__footer">
          <span>最近更新：{{ detail.updatedAt }}</span>
          <AppButton variant="primary" @click="handleSave">保存文档</AppButton>
        </div>
      </div>
      <div v-else class="document-empty">文档不存在或已被删除</div>
    </el-drawer>
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

.document-detail {
  display: grid;
  gap: 1rem;
}

.document-detail__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.document-empty {
  display: grid;
  place-items: center;
  min-height: 12rem;
  color: var(--color-text-muted);
}
</style>
