import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addFriend,
  createGroup,
  getFriendRequests,
  getFriends,
  getGroups,
  getSentFriendRequests,
  getUsers,
  handleFriendRequest,
  isDemoMode,
} from '../../../api/workspace'
import { useUserStore } from '../../../stores/user'

const friendPageSize = 6
const groupPageSize = 5

function formatStatus(status) {
  if (status === 1 || status === '1' || status === 'active') {
    return '启用'
  }
  if (status === 0 || status === '0' || status === 'disabled') {
    return '禁用'
  }
  return '未知'
}

function normalizeFriend(row) {
  const friendUser = row?.friendUser || row || {}

  return {
    id: row?.friendId ?? row?.id ?? friendUser.userId,
    userId: friendUser.userId ?? row?.userId ?? row?.id,
    name: friendUser.nickname || friendUser.username || row?.name || '未命名用户',
    account: friendUser.username || row?.account || '-',
    department: friendUser.departmentName || row?.department || '未分配部门',
    status: row?.status || formatStatus(friendUser.status),
    lastSeen: row?.lastSeen || row?.addTime || friendUser.createTime || '暂无记录',
  }
}

function normalizeGroup(row) {
  return {
    id: row?.groupId ?? row?.id,
    name: row?.groupName ?? row?.name ?? '未命名群聊',
    ownerId: row?.ownerId,
    ownerName: row?.ownerName || '',
    memberCount: row?.memberCount ?? 0,
    notice: row?.notice || '暂无群公告',
    updatedAt: row?.updatedAt || row?.createTime || '暂无记录',
  }
}

function normalizeIncomingRequest(row) {
  const requestUser = row?.requestUser || {}

  return {
    requestId: row?.requestId,
    userId: requestUser.userId,
    name: requestUser.nickname || requestUser.username || '未命名用户',
    account: requestUser.username || '-',
    department: requestUser.departmentName || '未分配部门',
    message: row?.message || '无附言',
    requestTime: row?.requestTime || '暂无记录',
    status: row?.status || 'pending',
  }
}

function normalizeOutgoingRequest(row) {
  const targetUser = row?.targetUser || {}

  return {
    requestId: row?.requestId,
    userId: targetUser.userId,
    name: targetUser.nickname || targetUser.username || '未命名用户',
    account: targetUser.username || '-',
    department: targetUser.departmentName || '未分配部门',
    message: row?.message || '无附言',
    requestTime: row?.requestTime || '暂无记录',
    status: row?.status || 'pending',
  }
}

function normalizeUser(item) {
  const userId = item?.userId ?? item?.id
  const username = item?.username ?? item?.account ?? ''
  const nickname = item?.nickname ?? item?.name ?? ''
  const departmentName = item?.departmentName ?? item?.department ?? ''

  return {
    userId,
    username,
    nickname,
    departmentName,
    status: item?.status,
  }
}

function normalizeUserOption(user) {
  return {
    value: user.userId,
    label: `${user.nickname || user.username || `用户#${user.userId}`}${user.departmentName ? ` · ${user.departmentName}` : ''}`,
  }
}

export function useContactsDirectory() {
  const userStore = useUserStore()
  const keyword = ref('')
  const groupKeyword = ref('')
  const friendPage = ref(1)
  const groupPage = ref(1)
  const friends = ref([])
  const groups = ref([])
  const incomingRequests = ref([])
  const outgoingRequests = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const demoData = ref(isDemoMode())
  const friendDialog = ref(false)
  const groupDialog = ref(false)
  const requestDialog = ref(false)
  const requestTab = ref('incoming')
  const requestActionLoading = ref('')
  const selectableUsers = ref([])
  const userOptionsLoading = ref(false)
  const userOptionsError = ref('')

  const currentUserId = computed(() => {
    const value = userStore.user?.id ?? userStore.user?.userId
    const normalized = Number(value)
    return Number.isFinite(normalized) ? normalized : null
  })

  const addFriendUserOptions = computed(() => {
    const friendIds = new Set(
      friends.value.map((item) => Number(item.userId)).filter(Number.isFinite),
    )
    const outgoingIds = new Set(
      outgoingRequests.value
        .filter((item) => item.status === 'pending')
        .map((item) => Number(item.userId))
        .filter(Number.isFinite),
    )

    return selectableUsers.value
      .filter((user) => Number(user.userId) !== currentUserId.value)
      .filter((user) => !friendIds.has(Number(user.userId)))
      .filter((user) => !outgoingIds.has(Number(user.userId)))
      .map(normalizeUserOption)
  })

  const filteredFriends = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) {
      return friends.value
    }

    return friends.value.filter((friend) =>
      [friend.name, friend.account, friend.department].some((field) =>
        String(field || '')
          .toLowerCase()
          .includes(value),
      ),
    )
  })

  const pagedFriends = computed(() => {
    const start = (friendPage.value - 1) * friendPageSize
    return filteredFriends.value.slice(start, start + friendPageSize)
  })

  const filteredGroups = computed(() => {
    const value = groupKeyword.value.trim().toLowerCase()
    if (!value) {
      return groups.value
    }

    return groups.value.filter((group) =>
      [group.name, group.notice].some((field) =>
        String(field || '')
          .toLowerCase()
          .includes(value),
      ),
    )
  })

  const pagedGroups = computed(() => {
    const start = (groupPage.value - 1) * groupPageSize
    return filteredGroups.value.slice(start, start + groupPageSize)
  })

  const pendingIncomingCount = computed(
    () => incomingRequests.value.filter((item) => item.status === 'pending').length,
  )

  async function loadData() {
    loading.value = true
    loadError.value = ''
    try {
      const [friendResult, groupResult, incomingResult, outgoingResult] = await Promise.all([
        getFriends({ page: 1, size: 100, keyword: keyword.value }),
        getGroups({ page: 1, size: 100 }),
        getFriendRequests({ page: 1, size: 100 }),
        getSentFriendRequests({ page: 1, size: 100 }),
      ])
      friends.value = (friendResult.data.records || []).map(normalizeFriend)
      groups.value = (groupResult.data.records || []).map(normalizeGroup)
      incomingRequests.value = (incomingResult.data.records || []).map(normalizeIncomingRequest)
      outgoingRequests.value = (outgoingResult.data.records || []).map(normalizeOutgoingRequest)
      demoData.value = [friendResult, groupResult, incomingResult, outgoingResult].some(
        (result) => result.source === 'demo',
      )
      const degraded = [friendResult, groupResult, incomingResult, outgoingResult].find(
        (result) => result.degraded,
      )
      if (degraded) {
        loadError.value = `接口暂不可用，已切换演示数据：${degraded.error || '请稍后重试'}`
      }
      friendPage.value = 1
      groupPage.value = 1
    } catch (error) {
      loadError.value = error.message || '通讯录数据加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadSelectableUsers() {
    userOptionsLoading.value = true
    userOptionsError.value = ''

    try {
      const response = await getUsers({ page: 1, size: 500 })
      selectableUsers.value = (response?.data?.records || []).map(normalizeUser)
    } catch (error) {
      selectableUsers.value = []
      userOptionsError.value = error.message || '用户列表加载失败'
    } finally {
      userOptionsLoading.value = false
    }
  }

  async function handleAddFriend(form) {
    const result = await addFriend(form)
    friendDialog.value = false
    ElMessage[result.degraded ? 'warning' : 'success'](
      result.degraded ? '好友接口不可用，申请已记入演示流程' : '好友申请已发送',
    )
    await loadData()
  }

  async function handleCreateGroup(form) {
    const result = await createGroup(form)
    groupDialog.value = false
    ElMessage[result.degraded ? 'warning' : 'success'](
      result.degraded ? '群聊接口不可用，已创建演示群聊' : '群聊已创建',
    )
    await loadData()
  }

  async function handleIncomingRequest(requestId, action) {
    requestActionLoading.value = `${action}-${requestId}`
    try {
      const result = await handleFriendRequest(requestId, action)
      ElMessage[result?.degraded ? 'warning' : 'success'](
        action === 'accept' ? '已同意好友申请' : '已拒绝好友申请',
      )
      await loadData()
    } catch (error) {
      ElMessage.error(error.message || '处理好友申请失败')
    } finally {
      requestActionLoading.value = ''
    }
  }

  function resetFriendPage() {
    friendPage.value = 1
  }

  function resetGroupPage() {
    groupPage.value = 1
  }

  onMounted(() => {
    loadData()
    loadSelectableUsers()
  })

  return {
    keyword,
    groupKeyword,
    friendPage,
    groupPage,
    friendPageSize,
    groupPageSize,
    friends,
    groups,
    incomingRequests,
    outgoingRequests,
    loading,
    loadError,
    demoData,
    friendDialog,
    groupDialog,
    requestDialog,
    requestTab,
    requestActionLoading,
    selectableUsers,
    userOptionsLoading,
    userOptionsError,
    currentUserId,
    addFriendUserOptions,
    filteredFriends,
    pagedFriends,
    filteredGroups,
    pagedGroups,
    pendingIncomingCount,
    loadData,
    loadSelectableUsers,
    handleAddFriend,
    handleCreateGroup,
    handleIncomingRequest,
    resetFriendPage,
    resetGroupPage,
  }
}
