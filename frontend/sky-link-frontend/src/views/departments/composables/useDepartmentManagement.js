import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDialog } from '../../../composables/useConfirmDialog'
import {
  addDepartmentMembers,
  createDepartment,
  deleteDepartment,
  getDepartmentMembers,
  getDepartments,
  getUsers,
  removeDepartmentMember,
  updateDepartment,
} from '../../../api/workspace'

const pageSize = 10
const membersPageSize = 8

const columns = [
  { key: 'departmentId', label: '部门ID', width: '90px' },
  { key: 'departmentName', label: '部门名称' },
  { key: 'leaderName', label: '负责人' },
  { key: 'memberCount', label: '成员数', slot: 'memberCount', width: '110px' },
  { key: 'description', label: '说明' },
  { key: 'actions', label: '操作', slot: 'actions', width: '260px', align: 'right' },
]

function unwrapData(response) {
  return response?.data ?? response ?? {}
}

function normalizePage(data) {
  if (Array.isArray(data)) {
    return { records: data, total: data.length, page: 1, size: data.length }
  }

  return {
    records: data.records || [],
    total: data.total || 0,
    page: data.page || 1,
    size: data.size || 0,
  }
}

function normalizeDepartment(item) {
  const departmentId = item.departmentId ?? item.id

  return {
    departmentId,
    departmentName: item.departmentName ?? item.name ?? '',
    leaderId: item.leaderId ?? '',
    leaderName: item.leaderName ?? item.leader ?? '',
    description: item.description ?? '',
    memberCount: Number(item.memberCount ?? 0),
  }
}

function normalizeUser(item) {
  return {
    userId: item.userId ?? item.id,
    username: item.username ?? item.account ?? '',
    nickname: item.nickname ?? item.name ?? '',
    email: item.email ?? '-',
    phone: item.phone ?? '-',
    status: item.status,
    departmentId: item.departmentId ?? '',
    departmentName: item.departmentName ?? item.department ?? '',
  }
}

export function useDepartmentManagement() {
  const page = ref(1)
  const rows = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const keyword = ref('')
  const leaderOptions = ref([])

  const formVisible = ref(false)
  const formMode = ref('create')
  const formSaving = ref(false)
  const editingDepartmentId = ref(null)
  const departmentForm = reactive({
    departmentName: '',
    leaderId: '',
    description: '',
  })

  const membersVisible = ref(false)
  const membersLoading = ref(false)
  const membersError = ref('')
  const membersPage = ref(1)
  const membersTotal = ref(0)
  const memberRows = ref([])
  const activeDepartment = ref(null)
  const addMembersVisible = ref(false)
  const addMembersSaving = ref(false)
  const selectedMemberIds = ref([])
  const { confirm } = useConfirmDialog()

  const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) {
      return rows.value
    }

    return rows.value.filter((row) =>
      [row.departmentName, row.leaderName, row.description]
        .some((field) => String(field || '').toLowerCase().includes(value)),
    )
  })

  const pagedRows = computed(() => {
    const start = (page.value - 1) * pageSize
    return filteredRows.value.slice(start, start + pageSize)
  })

  const formTitle = computed(() => (formMode.value === 'create' ? '新建部门' : '编辑部门'))
  const formConfirmText = computed(() => (formMode.value === 'create' ? '创建部门' : '保存修改'))
  const availableMemberOptions = computed(() => {
    if (!activeDepartment.value) {
      return []
    }

    return leaderOptions.value.filter((user) => {
      if (user.userId == null) {
        return false
      }
      if (user.departmentId != null && activeDepartment.value.departmentId != null) {
        return Number(user.departmentId) !== Number(activeDepartment.value.departmentId)
      }
      return user.departmentName !== activeDepartment.value.departmentName
    })
  })

  function formatStatus(value) {
    if (value === 1 || value === 'active' || value === '启用') {
      return { label: '启用', tone: 'success' }
    }
    if (value === 0 || value === 'disabled' || value === '禁用') {
      return { label: '禁用', tone: 'danger' }
    }
    return { label: '未知', tone: 'default' }
  }

  async function loadDepartments() {
    loading.value = true
    loadError.value = ''

    try {
      const response = await getDepartments({ page: 1, size: 500, keyword: '' })
      const data = unwrapData(response)
      const pageData = normalizePage(data)
      rows.value = pageData.records.map(normalizeDepartment)
      if (page.value > 1 && pagedRows.value.length === 0) {
        page.value = Math.max(Math.ceil(filteredRows.value.length / pageSize), 1)
      }
    } catch (error) {
      loadError.value = error.message || '部门列表加载失败'
    } finally {
      loading.value = false
    }
  }

  async function loadLeaderOptions() {
    try {
      const response = await getUsers({ page: 1, size: 500 })
      const data = unwrapData(response)
      leaderOptions.value = normalizePage(data).records.map(normalizeUser)
    } catch {
      leaderOptions.value = []
    }
  }

  async function refreshAll() {
    await Promise.allSettled([
      loadDepartments(),
      loadLeaderOptions(),
    ])
  }

  function handleSearch() {
    page.value = 1
  }

  function handleReset() {
    keyword.value = ''
    page.value = 1
    loadDepartments()
  }

  function resetForm() {
    editingDepartmentId.value = null
    departmentForm.departmentName = ''
    departmentForm.leaderId = ''
    departmentForm.description = ''
  }

  function openCreateDialog() {
    formMode.value = 'create'
    resetForm()
    formVisible.value = true
  }

  function openEditDialog(row) {
    formMode.value = 'edit'
    editingDepartmentId.value = row.departmentId
    departmentForm.departmentName = row.departmentName
    departmentForm.leaderId = row.leaderId || ''
    departmentForm.description = row.description || ''
    formVisible.value = true
  }

  function syncFormData(source = departmentForm) {
    departmentForm.departmentName = source?.departmentName ?? ''
    departmentForm.leaderId = source?.leaderId ?? ''
    departmentForm.description = source?.description ?? ''
  }

  function buildPayload() {
    return {
      departmentName: departmentForm.departmentName.trim(),
      leaderId: departmentForm.leaderId === '' ? undefined : Number(departmentForm.leaderId),
      description: departmentForm.description.trim() || undefined,
    }
  }

  async function saveDepartment(submittedForm) {
    if (submittedForm) {
      syncFormData(submittedForm)
    }

    const payload = buildPayload()
    if (!payload.departmentName) {
      ElMessage.warning('请填写部门名称')
      return
    }

    formSaving.value = true
    try {
      if (formMode.value === 'create') {
        await createDepartment(payload)
        ElMessage.success('部门已创建')
      } else {
        await updateDepartment(editingDepartmentId.value, payload)
        ElMessage.success('部门已更新')
      }

      formVisible.value = false
      await loadDepartments()
    } catch (error) {
      ElMessage.error(error.message || '保存部门失败')
    } finally {
      formSaving.value = false
    }
  }

  async function removeDepartment(row) {
    try {
      await confirm(`确定要删除部门「${row.departmentName}」吗？`, '删除部门', {
        confirmText: '删除',
        cancelText: '取消',
        type: 'danger',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    try {
      await deleteDepartment(row.departmentId)
      ElMessage.success('部门已删除')
      if (pagedRows.value.length === 1 && page.value > 1) {
        page.value -= 1
      }
      await loadDepartments()
    } catch (error) {
      ElMessage.error(error.message || '删除部门失败')
    }
  }

  async function openMembers(row) {
    activeDepartment.value = row
    membersVisible.value = true
    membersPage.value = 1
    await loadMembers(1)
  }

  async function openAddMembers() {
    selectedMemberIds.value = []
    if (!leaderOptions.value.length) {
      await loadLeaderOptions()
    }
    addMembersVisible.value = true
  }

  async function loadMembers(targetPage = membersPage.value) {
    if (!activeDepartment.value) {
      return
    }

    membersLoading.value = true
    membersError.value = ''

    try {
      const response = await getDepartmentMembers(activeDepartment.value.departmentId, {
        page: targetPage,
        size: membersPageSize,
      })
      const data = normalizePage(unwrapData(response))
      memberRows.value = data.records.map(normalizeUser)
      membersTotal.value = data.total
      membersPage.value = data.page || targetPage
    } catch (error) {
      membersError.value = error.message || '部门成员加载失败'
    } finally {
      membersLoading.value = false
    }
  }

  async function saveMembers() {
    if (!activeDepartment.value) {
      return
    }
    if (!selectedMemberIds.value.length) {
      ElMessage.warning('请选择要加入的成员')
      return
    }

    addMembersSaving.value = true
    try {
      await addDepartmentMembers(activeDepartment.value.departmentId, selectedMemberIds.value)
      ElMessage.success('成员已加入部门')
      addMembersVisible.value = false
      selectedMemberIds.value = []
      await Promise.all([
        loadMembers(1),
        loadDepartments(),
        loadLeaderOptions(),
      ])
    } catch (error) {
      ElMessage.error(error.message || '加入成员失败')
    } finally {
      addMembersSaving.value = false
    }
  }

  async function removeMember(row) {
    if (!activeDepartment.value) {
      return
    }

    try {
      await confirm(`确定要将「${row.nickname || row.username}」移出「${activeDepartment.value.departmentName}」吗？`, '移出成员', {
        confirmText: '移出',
        cancelText: '取消',
        type: 'danger',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    try {
      await removeDepartmentMember(activeDepartment.value.departmentId, row.userId)
      ElMessage.success('成员已移出')
      if (memberRows.value.length === 1 && membersPage.value > 1) {
        membersPage.value -= 1
      }
      await Promise.all([
        loadMembers(membersPage.value),
        loadDepartments(),
        loadLeaderOptions(),
      ])
    } catch (error) {
      ElMessage.error(error.message || '移出成员失败')
    }
  }

  watch(
    () => filteredRows.value.length,
    (length) => {
      const maxPage = Math.max(Math.ceil(length / pageSize), 1)
      if (page.value > maxPage) {
        page.value = maxPage
      }
    },
  )

  return {
    page,
    pageSize,
    rows,
    loading,
    loadError,
    keyword,
    leaderOptions,
    formVisible,
    formMode,
    formSaving,
    editingDepartmentId,
    departmentForm,
    membersVisible,
    membersLoading,
    membersError,
    membersPage,
    membersPageSize,
    membersTotal,
    memberRows,
    activeDepartment,
    addMembersVisible,
    addMembersSaving,
    selectedMemberIds,
    columns,
    filteredRows,
    pagedRows,
    formTitle,
    formConfirmText,
    availableMemberOptions,
    formatStatus,
    loadDepartments,
    loadLeaderOptions,
    refreshAll,
    handleSearch,
    handleReset,
    openCreateDialog,
    openEditDialog,
    saveDepartment,
    removeDepartment,
    openMembers,
    openAddMembers,
    loadMembers,
    saveMembers,
    removeMember,
  }
}
