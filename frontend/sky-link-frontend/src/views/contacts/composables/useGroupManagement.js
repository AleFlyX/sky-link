import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDialog } from '../../../composables/useConfirmDialog'
import {
  addGroupMembers as inviteGroupMembers,
  deleteGroup as dissolveGroup,
  getGroup as getGroupDetail,
  getGroupMembers,
  leaveGroup as leaveManagedGroup,
  removeGroupMember as removeManagedGroupMember,
  updateGroup as updateManagedGroup,
  updateGroupMemberRole as updateManagedGroupMemberRole,
} from '../../../api/workspace'

const groupMembersPageSize = 6

function normalizeGroupMember(row) {
  return {
    userId: row?.userId,
    username: row?.username || '-',
    nickname: row?.nickname || row?.username || `用户#${row?.userId || ''}`,
    role: row?.role || 'member',
    joinTime: row?.joinTime || '暂无记录',
  }
}

function normalizeGroupDetail(row) {
  return {
    id: row?.groupId ?? row?.id,
    name: row?.groupName ?? row?.name ?? '未命名群聊',
    ownerId: row?.ownerId,
    ownerName: row?.ownerName || '未知用户',
    memberCount: row?.memberCount ?? 0,
    notice: row?.notice || '',
    createTime: row?.createTime || '暂无记录',
    members: Array.isArray(row?.members) ? row.members.map(normalizeGroupMember) : [],
  }
}

export function useGroupManagement({ getCurrentUserId, reloadContacts }) {
  const groupManageDialog = ref(false)
  const inviteGroupMembersDialog = ref(false)
  const activeGroupId = ref(null)
  const groupDetail = ref(null)
  const groupDetailLoading = ref(false)
  const groupDetailError = ref('')
  const groupMembers = ref([])
  const groupMembersLoading = ref(false)
  const groupMembersError = ref('')
  const groupMembersPage = ref(1)
  const groupMembersTotal = ref(0)
  const groupActionLoading = ref('')
  const { confirm } = useConfirmDialog()

  const currentGroupRole = computed(() => {
    if (!groupDetail.value?.members?.length) {
      return ''
    }

    return (
      groupDetail.value.members.find((member) => member.userId === getCurrentUserId())?.role || ''
    )
  })

  async function loadGroupDetailData(groupId = activeGroupId.value) {
    if (!groupId) {
      return
    }

    groupDetailLoading.value = true
    groupDetailError.value = ''
    try {
      const result = await getGroupDetail(groupId)
      groupDetail.value = normalizeGroupDetail(result.data || result)
    } catch (error) {
      groupDetailError.value = error.message || '群详情加载失败'
    } finally {
      groupDetailLoading.value = false
    }
  }

  async function loadGroupMembersData(
    groupId = activeGroupId.value,
    page = groupMembersPage.value,
  ) {
    if (!groupId) {
      return
    }

    groupMembersLoading.value = true
    groupMembersError.value = ''
    try {
      const result = await getGroupMembers(groupId, { page, size: groupMembersPageSize })
      groupMembers.value = (result.data.records || []).map(normalizeGroupMember)
      groupMembersTotal.value = result.data.total || 0
    } catch (error) {
      groupMembersError.value = error.message || '群成员加载失败'
    } finally {
      groupMembersLoading.value = false
    }
  }

  async function openGroupManage(group) {
    activeGroupId.value = group.id ?? group.groupId
    groupManageDialog.value = true
    groupDetail.value = null
    groupMembers.value = []
    groupMembersPage.value = 1
    await Promise.all([
      loadGroupDetailData(activeGroupId.value),
      loadGroupMembersData(activeGroupId.value, 1),
    ])
  }

  async function refreshActiveGroupData() {
    if (!activeGroupId.value) {
      return
    }

    await Promise.all([
      loadGroupDetailData(activeGroupId.value),
      loadGroupMembersData(activeGroupId.value, groupMembersPage.value),
      reloadContacts(),
    ])
  }

  async function handleSaveGroup(form) {
    if (!activeGroupId.value) {
      return
    }

    groupActionLoading.value = 'save-group'
    try {
      const result = await updateManagedGroup(activeGroupId.value, form)
      groupDetail.value = normalizeGroupDetail(result.data || result)
      ElMessage.success('群信息已更新')
      await Promise.all([
        loadGroupMembersData(activeGroupId.value, groupMembersPage.value),
        reloadContacts(),
      ])
    } catch (error) {
      ElMessage.error(error.message || '更新群信息失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  async function handleInviteGroupMembers(userIds) {
    if (!activeGroupId.value) {
      return
    }

    groupActionLoading.value = 'invite-members'
    try {
      await inviteGroupMembers(activeGroupId.value, { userIds })
      inviteGroupMembersDialog.value = false
      ElMessage.success('已邀请成员入群')
      groupMembersPage.value = 1
      await Promise.all([
        loadGroupDetailData(activeGroupId.value),
        loadGroupMembersData(activeGroupId.value, 1),
        reloadContacts(),
      ])
    } catch (error) {
      ElMessage.error(error.message || '邀请成员失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  async function handleRemoveGroupMember(member) {
    if (!activeGroupId.value) {
      return
    }

    try {
      await confirm(`确认将 ${member.nickname || member.username} 移出群聊吗？`, '移除成员', {
        confirmText: '移除',
        cancelText: '取消',
        type: 'danger',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    groupActionLoading.value = `remove-member-${member.userId}`
    try {
      await removeManagedGroupMember(activeGroupId.value, member.userId)
      ElMessage.success('成员已移除')
      await refreshActiveGroupData()
    } catch (error) {
      ElMessage.error(error.message || '移除成员失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  async function handleUpdateGroupMemberRole(member, role) {
    if (!activeGroupId.value) {
      return
    }

    const message =
      role === 'admin'
        ? `确认将 ${member.nickname || member.username} 设为管理员吗？`
        : `确认取消 ${member.nickname || member.username} 的管理员身份吗？`

    try {
      await confirm(message, '修改成员角色', {
        confirmText: role === 'admin' ? '设为管理员' : '取消管理员',
        cancelText: '取消',
        type: 'warning',
        confirmVariant: 'primary',
      })
    } catch {
      return
    }

    groupActionLoading.value = `role-${role}-${member.userId}`
    try {
      await updateManagedGroupMemberRole(activeGroupId.value, member.userId, role)
      ElMessage.success(role === 'admin' ? '已设为管理员' : '已取消管理员')
      await refreshActiveGroupData()
    } catch (error) {
      ElMessage.error(error.message || '修改成员角色失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  async function handleLeaveGroup() {
    if (!activeGroupId.value) {
      return
    }

    try {
      await confirm('确认退出当前群聊吗？', '退出群聊', {
        confirmText: '退出',
        cancelText: '取消',
        type: 'warning',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    groupActionLoading.value = 'leave-group'
    try {
      await leaveManagedGroup(activeGroupId.value)
      ElMessage.success('已退出群聊')
      groupManageDialog.value = false
      inviteGroupMembersDialog.value = false
      await reloadContacts()
    } catch (error) {
      ElMessage.error(error.message || '退出群聊失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  async function handleDissolveGroup() {
    if (!activeGroupId.value) {
      return
    }

    try {
      await confirm('解散后成员将被移除，确认继续吗？', '解散群组', {
        confirmText: '解散',
        cancelText: '取消',
        type: 'danger',
        confirmVariant: 'danger',
      })
    } catch {
      return
    }

    groupActionLoading.value = 'dissolve-group'
    try {
      await dissolveGroup(activeGroupId.value)
      ElMessage.success('群组已解散')
      groupManageDialog.value = false
      inviteGroupMembersDialog.value = false
      await reloadContacts()
    } catch (error) {
      ElMessage.error(error.message || '解散群组失败')
    } finally {
      groupActionLoading.value = ''
    }
  }

  return {
    groupManageDialog,
    inviteGroupMembersDialog,
    activeGroupId,
    groupDetail,
    groupDetailLoading,
    groupDetailError,
    groupMembers,
    groupMembersLoading,
    groupMembersError,
    groupMembersPage,
    groupMembersPageSize,
    groupMembersTotal,
    groupActionLoading,
    currentGroupRole,
    loadGroupDetailData,
    loadGroupMembersData,
    openGroupManage,
    refreshActiveGroupData,
    handleSaveGroup,
    handleInviteGroupMembers,
    handleRemoveGroupMember,
    handleUpdateGroupMemberRole,
    handleLeaveGroup,
    handleDissolveGroup,
  }
}
