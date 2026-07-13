<script setup>
import { computed, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import { files } from '../../mock/workspace'

const keyword = ref('')
const page = ref(1)
const pageSize = 5
const dialogVisible = ref(false)

const columns = [
  { key: 'name', label: '文件名称' },
  { key: 'category', label: '分类' },
  { key: 'owner', label: '上传人' },
  { key: 'visibility', label: '可见范围' },
  { key: 'size', label: '文件大小' },
  { key: 'updatedAt', label: '更新时间' },
]

const filteredRows = computed(() =>
  files.filter((item) =>
    [item.name, item.category, item.owner].some((text) =>
      text.toLowerCase().includes(keyword.value.toLowerCase()),
    ),
  ),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRows.value.slice(start, start + pageSize)
})
</script>

<template>
  <div class="page-shell">
    <AppCard
      variant="default"
      title="文件中心"
      subtitle="成员 B 联调：文件列表、上传入口与可见范围展示已就绪"
    >
      <div class="page-toolbar">
        <el-input v-model="keyword" placeholder="搜索文件名 / 分类 / 上传人" clearable />
        <AppButton variant="primary" @click="dialogVisible = true">上传文件</AppButton>
      </div>

      <AppDataTable :columns="columns" :rows="pagedRows" empty-text="暂无文件数据" />
      <AppPagination v-model:page="page" :page-size="pageSize" :total="filteredRows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="上传文件"
      confirm-text="保存记录"
      :fields="[
        { key: 'name', label: '文件名称' },
        { key: 'category', label: '文件分类' },
        { key: 'visibility', label: '可见范围' },
      ]"
      :form-data="{ name: '', category: '', visibility: '团队可见' }"
      @submit="() => {}"
    />
  </div>
</template>
