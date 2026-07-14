<script setup>
import { onMounted, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import { createSchedule, getSchedules, isDemoMode } from '../../api/workspace'

const keyword = ref('')
const page = ref(1)
const pageSize = 6
const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())
const dialogVisible = ref(false)

const columns = [
  { key: 'title', label: '日程主题' },
  { key: 'startTime', label: '开始时间' },
  { key: 'endTime', label: '结束时间' },
  { key: 'repeatType', label: '重复' },
  { key: 'owner', label: '创建人' },
]

async function loadData() {
  loading.value = true
  loadError.value = ''
  const result = await getSchedules({ page: 1, size: 100, keyword: keyword.value })
  rows.value = result.data.records || []
  demoData.value = result.source === 'demo'
  if (result.degraded) loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
  loading.value = false
}

async function handleCreate(form) {
  const result = await createSchedule(form)
  dialogVisible.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '日程接口不可用，已创建演示日程' : '日程已创建')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <AppCard title="日程安排" subtitle="查看近期日程并快速创建个人或团队安排">
      <div class="page-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索日程主题 / 内容" @keyup.enter="loadData" />
        <AppButton variant="primary" @click="dialogVisible = true">创建日程</AppButton>
      </div>

      <el-alert v-if="demoData" title="当前为演示数据模式，创建日程后会立即出现在列表顶部" type="info" show-icon :closable="false" class="page-feedback" />
      <AppDataTable :columns="columns" :rows="rows" :loading="loading" :error="loadError" empty-text="暂无日程" @retry="loadData" />
      <AppPagination v-model:page="page" :page-size="pageSize" :total="rows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="创建日程"
      confirm-text="保存日程"
      :fields="[
        { key: 'title', label: '日程主题', required: true },
        { key: 'startTime', label: '开始时间', placeholder: '2026-07-14 09:00' },
        { key: 'endTime', label: '结束时间', placeholder: '2026-07-14 10:00' },
        { key: 'repeatType', label: '重复方式', type: 'select', options: [{ value: 'none', label: '不重复' }, { value: 'daily', label: '每天' }, { value: 'weekly', label: '每周' }, { value: 'monthly', label: '每月' }] },
        { key: 'content', label: '日程说明', type: 'textarea' },
      ]"
      :form-data="{ title: '', startTime: '', endTime: '', repeatType: 'none', content: '' }"
      @submit="handleCreate"
    />
  </div>
</template>
