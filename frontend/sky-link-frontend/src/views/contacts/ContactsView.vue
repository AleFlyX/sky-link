<script setup>
import { onMounted, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppDataTable from '../../components/common/AppDataTable.vue'
import AppFormDialog from '../../components/common/AppFormDialog.vue'
import AppPagination from '../../components/common/AppPagination.vue'
import { addFriend, createGroup, getFriends, getGroups, isDemoMode } from '../../api/workspace'

const keyword = ref('')
const page = ref(1)
const pageSize = 6
const friends = ref([])
const groups = ref([])
const loading = ref(false)
const loadError = ref('')
const demoData = ref(isDemoMode())
const friendDialog = ref(false)
const groupDialog = ref(false)

const friendColumns = [
  { key: 'name', label: '好友' },
  { key: 'account', label: '账号' },
  { key: 'department', label: '部门' },
  { key: 'status', label: '状态' },
  { key: 'lastSeen', label: '最近活跃' },
]

const groupColumns = [
  { key: 'name', label: '群聊名称' },
  { key: 'memberCount', label: '成员数' },
  { key: 'notice', label: '群公告' },
  { key: 'updatedAt', label: '最近更新' },
]

async function loadData() {
  loading.value = true
  loadError.value = ''
  const [friendResult, groupResult] = await Promise.all([
    getFriends({ page: 1, size: 100, keyword: keyword.value }),
    getGroups({ page: 1, size: 100 }),
  ])
  friends.value = friendResult.data.records || []
  groups.value = groupResult.data.records || []
  demoData.value = friendResult.source === 'demo' || groupResult.source === 'demo'
  const degraded = friendResult.degraded || groupResult.degraded
  if (degraded) loadError.value = `接口暂不可用，已切换演示数据：${friendResult.error || groupResult.error}`
  loading.value = false
}

async function handleAddFriend(form) {
  const result = await addFriend(form)
  friendDialog.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '好友接口不可用，申请已记入演示流程' : '好友申请已发送')
}

async function handleCreateGroup(form) {
  const result = await createGroup(form)
  groupDialog.value = false
  ElMessage[result.degraded ? 'warning' : 'success'](result.degraded ? '群聊接口不可用，已创建演示群聊' : '群聊已创建')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-grid contacts-page">
    <AppCard title="好友通讯录" subtitle="搜索成员、查看在线状态并发起好友申请">
      <div class="page-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索姓名 / 账号 / 部门" @keyup.enter="loadData" />
        <AppButton variant="primary" @click="friendDialog = true">添加好友</AppButton>
      </div>

      <el-alert v-if="demoData" title="当前为演示数据模式，好友申请会即时反馈" type="info" show-icon :closable="false" class="page-feedback" />
      <AppDataTable :columns="friendColumns" :rows="friends" :loading="loading" :error="loadError" empty-text="暂无好友" @retry="loadData" />
      <AppPagination v-model:page="page" :page-size="pageSize" :total="friends.length" />
    </AppCard>

    <AppCard title="群聊空间" subtitle="集中查看团队群聊和最近群公告">
      <div class="page-toolbar">
        <span class="section-hint">共 {{ groups.length }} 个群聊</span>
        <AppButton variant="primary" @click="groupDialog = true">创建群聊</AppButton>
      </div>
      <AppDataTable :columns="groupColumns" :rows="groups" :loading="loading" empty-text="暂无群聊" />
    </AppCard>

    <AppFormDialog
      v-model="friendDialog"
      title="添加好友"
      confirm-text="发送申请"
      :fields="[
        { key: 'friendUserId', label: '好友用户 ID', placeholder: '例如 1002', required: true },
        { key: 'message', label: '申请附言', placeholder: '介绍一下你自己', type: 'textarea' },
      ]"
      :form-data="{ friendUserId: '', message: '' }"
      @submit="handleAddFriend"
    />

    <AppFormDialog
      v-model="groupDialog"
      title="创建群聊"
      confirm-text="创建群聊"
      :fields="[
        { key: 'name', label: '群聊名称', required: true },
        { key: 'notice', label: '群公告', type: 'textarea' },
      ]"
      :form-data="{ name: '', notice: '' }"
      @submit="handleCreateGroup"
    />
  </div>
</template>

<style scoped>
.contacts-page {
  align-items: start;
}

.section-hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
