<script setup>
import { computed, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import { departments } from '../../mock/workspace'

const keyword = ref('')
const page = ref(1)
const pageSize = 4
const dialogVisible = ref(false)

const columns = [
  { key: 'name', label: '部门名称' },
  { key: 'leader', label: '负责人' },
  { key: 'memberCount', label: '成员数' },
  { key: 'roleScope', label: '角色范围' },
  { key: 'description', label: '说明' },
]

const filteredRows = computed(() =>
  departments.filter((item) =>
    [item.name, item.leader, item.roleScope].some((text) =>
      text.toLowerCase().includes(keyword.value.toLowerCase()),
    ),
  ),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRows.value.slice(start, start + pageSize)
})

function openDialog() {
  dialogVisible.value = true
}
</script>

<template>
  <div class="page-shell">
    <AppCard title="部门管理" subtitle="成员 A 联调：部门列表已可演示，后续可无缝替换接口">
      <div class="page-toolbar">
        <el-input v-model="keyword" placeholder="搜索部门 / 负责人 / 角色范围" clearable />
        <AppButton variant="primary" @click="openDialog">新建部门</AppButton>
      </div>

      <AppDataTable :columns="columns" :rows="pagedRows" empty-text="暂无部门数据" />
      <AppPagination v-model:page="page" :page-size="pageSize" :total="filteredRows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="新建部门"
      confirm-text="保存部门"
      :fields="[
        { key: 'name', label: '部门名称' },
        { key: 'leader', label: '负责人' },
        { key: 'roleScope', label: '角色范围' },
        { key: 'description', label: '部门说明', type: 'textarea' },
      ]"
      :form-data="{ name: '', leader: '', roleScope: '', description: '' }"
      @submit="() => {}"
    />
  </div>
</template>
