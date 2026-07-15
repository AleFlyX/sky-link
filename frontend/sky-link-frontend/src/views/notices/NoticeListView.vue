<script setup>
import { computed, onMounted, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppInput from '../../components/common/AppInput.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { createNotice, getNotices, isDemoMode, markNoticeRead } from '../../api/workspace'
import { noticeTypeMap, noticeTypeOptions } from '../../constants/enums'
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()
const keyword = ref('')
const type = ref('')
const page = ref(1)
const pageSize = 5
const dialogVisible = ref(false)
const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())

const columns = [
  { key: 'title', label: '公告标题' },
  { key: 'type', label: '公告类型', slot: 'type' },
  { key: 'publisher', label: '发布人' },
  { key: 'publishAt', label: '发布时间' },
  { key: 'read', label: '阅读状态', slot: 'read' },
]

const filteredRows = computed(() =>
  rows.value.filter((item) => {
    const matchKeyword = [item.title, item.publisher].some((text) =>
      text.toLowerCase().includes(keyword.value.toLowerCase()),
    )
    const matchType = !type.value || item.type === type.value
    return matchKeyword && matchType
  }),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRows.value.slice(start, start + pageSize)
})

function handlePublish() {
  dialogVisible.value = true
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  const result = await getNotices({ page: 1, size: 100 })
  rows.value = result.data.records || []
  demoData.value = result.source === 'demo'
  if (result.degraded) loadError.value = `接口暂不可用，已切换演示数据：${result.error}`
  loading.value = false
}

async function handleSubmit(form) {
  const result = await createNotice(form)
  ElMessage[result.degraded ? 'warning' : 'success'](
    result.degraded ? '接口暂不可用，已保存到演示数据' : '公告已发布',
  )
  dialogVisible.value = false
  await loadData()
}

async function handleRead(id) {
  await markNoticeRead(id)
  const notice = rows.value.find((item) => item.id === id)
  if (notice) notice.read = true
  appStore.markNotificationsRead()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <AppCard
      variant="default"
      title="公告通知"
      :subtitle="`当前共有 ${appStore.unreadNotificationCount} 条未读通知，铃铛按钮会实时响应`"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <AppInput v-model="keyword" placeholder="搜索公告标题 / 发布人" clearable />
          <el-select v-model="type" placeholder="筛选公告类型" clearable>
            <el-option
              v-for="item in noticeTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <AppButton variant="primary" @click="handlePublish">发布公告</AppButton>
      </div>

      <el-alert
        v-if="demoData"
        title="当前为演示数据模式，发布和已读状态会即时反馈"
        type="info"
        show-icon
        :closable="false"
        class="page-feedback"
      />

      <AppDataTable
        :columns="columns"
        :rows="pagedRows"
        :loading="loading"
        :error="loadError"
        empty-text="暂无公告数据"
        @retry="loadData"
      >
        <template #type="{ value }">
          <AppStatusTag :label="noticeTypeMap[value].label" :tone="noticeTypeMap[value].tone" />
        </template>

        <template #read="{ value, row }">
          <button type="button" class="notice-read" @click="!value && handleRead(row.id)">
            <AppStatusTag :label="value ? '已读' : '未读'" :tone="value ? 'info' : 'warning'" />
          </button>
        </template>
      </AppDataTable>

      <AppPagination v-model:page="page" :page-size="pageSize" :total="filteredRows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="发布公告"
      confirm-text="确认发布"
      :fields="[
        { key: 'title', label: '公告标题' },
        { key: 'type', label: '公告类型', type: 'select', options: noticeTypeOptions },
        { key: 'content', label: '公告内容', type: 'textarea' },
      ]"
      :form-data="{ title: '', type: 'system', content: '' }"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.notice-read {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}
</style>
