<script setup>
import { computed, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { userStatusMap, userStatusOptions } from '../../constants/enums'
import { users } from '../../mock/workspace'

const keyword = ref('')
const status = ref('')
const page = ref(1)
const pageSize = 5
const dialogVisible = ref(false)

const columns = [
  { key: 'name', label: '姓名' },
  { key: 'account', label: '账号' },
  { key: 'department', label: '所属部门' },
  { key: 'roles', label: '角色', slot: 'roles' },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'phone', label: '手机号' },
  { key: 'updatedAt', label: '最近更新' },
]

const filteredRows = computed(() =>
  users.filter((item) => {
    const matchKeyword = [item.name, item.account, item.department].some((text) =>
      text.toLowerCase().includes(keyword.value.toLowerCase()),
    )
    const matchStatus = !status.value || item.status === status.value
    return matchKeyword && matchStatus
  }),
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
    <AppCard
      variant="default"
      title="用户管理"
      subtitle="已串起用户列表、状态展示、部门信息与角色展示，可直接对接成员 A 的接口"
    >
      <div class="page-toolbar">
        <div class="page-toolbar__filters">
          <el-input v-model="keyword" placeholder="搜索姓名 / 账号 / 部门" clearable />
          <el-select v-model="status" placeholder="筛选状态" clearable>
            <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <AppButton variant="primary" @click="openDialog">新增用户</AppButton>
      </div>

      <AppDataTable :columns="columns" :rows="pagedRows" empty-text="暂无用户数据">
        <template #roles="{ row }">
          <div class="table-tags">
            <span v-for="role in row.roles" :key="role" class="table-chip">{{ role }}</span>
          </div>
        </template>

        <template #status="{ value }">
          <AppStatusTag :label="userStatusMap[value].label" :tone="userStatusMap[value].tone" />
        </template>
      </AppDataTable>

      <AppPagination v-model:page="page" :page-size="pageSize" :total="filteredRows.length" />
    </AppCard>

    <AppFormDialog
      v-model="dialogVisible"
      title="新增用户"
      confirm-text="保存用户"
      :fields="[
        { key: 'name', label: '姓名' },
        { key: 'account', label: '登录账号' },
        { key: 'department', label: '所属部门' },
        { key: 'status', label: '用户状态', type: 'select', options: userStatusOptions },
      ]"
      :form-data="{ name: '', account: '', department: '', status: 'active' }"
      @submit="() => {}"
    />
  </div>
</template>

<style scoped>
.table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.table-chip {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  background: #f1f5fb;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}
</style>
