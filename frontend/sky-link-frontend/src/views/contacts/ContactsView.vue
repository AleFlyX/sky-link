<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AddFriendDialog from './components/AddFriendDialog.vue'
import ContactsHeroPanel from './components/ContactsHeroPanel.vue'
import CreateGroupDialog from './components/CreateGroupDialog.vue'
import FriendDirectorySection from './components/FriendDirectorySection.vue'
import FriendRequestDialog from './components/FriendRequestDialog.vue'
import GroupManageDialog from './components/GroupManageDialog.vue'
import GroupSpaceSection from './components/GroupSpaceSection.vue'
import InviteGroupMembersDialog from './components/InviteGroupMembersDialog.vue'
import { useContactsDirectory } from './composables/useContactsDirectory'
import { useGroupManagement } from './composables/useGroupManagement'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const {
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
  addFriendUserOptions,
  filteredFriends,
  pagedFriends,
  filteredGroups,
  pagedGroups,
  pendingIncomingCount,
  loadData,
  handleAddFriend,
  handleCreateGroup,
  handleIncomingRequest,
  resetFriendPage,
  resetGroupPage,
} = useContactsDirectory()

const inviteGroupUserOptions = computed(() => {
  const memberIds = new Set(
    (groupDetail.value?.members || [])
      .map((member) => Number(member.userId))
      .filter(Number.isFinite),
  )

  return selectableUsers.value
    .filter((user) => !memberIds.has(Number(user.userId)))
    .map((user) => ({
      value: user.userId,
      label: `${user.nickname || user.username || `用户#${user.userId}`}${user.departmentName ? ` · ${user.departmentName}` : ''}`,
    }))
})

const groupColumns = [
  { key: 'name', label: '群聊名称' },
  { key: 'memberCount', label: '成员数' },
  { key: 'notice', label: '群公告' },
  { key: 'updatedAt', label: '最近更新' },
  { key: 'actions', label: '操作', width: '220px', align: 'center', slot: 'actions' },
]

const {
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
  handleSaveGroup,
  handleInviteGroupMembers,
  handleRemoveGroupMember,
  handleUpdateGroupMemberRole,
  handleLeaveGroup,
  handleDissolveGroup,
} = useGroupManagement({
  getCurrentUserId: () => Number(userStore.user.id) || 1001,
  reloadContacts: loadData,
})

function openSingleChat(friend) {
  const targetId = friend.userId ?? friend.id
  if (!targetId) {
    return
  }

  router.push({
    path: '/app/messages',
    query: {
      type: 'single',
      id: targetId,
      name: friend.name || `用户#${targetId}`,
    },
  })
}

function openGroupChat(group) {
  const targetId = group.id ?? group.groupId
  if (!targetId) {
    return
  }

  router.push({
    path: '/app/messages',
    query: {
      type: 'group',
      id: targetId,
      name: group.name || `群聊#${targetId}`,
    },
  })
}
</script>

<template>
  <div class="page-shell contacts-page">
    <ContactsHeroPanel
      :keyword="keyword"
      :friends-count="friends.length"
      :groups-count="groups.length"
      :incoming-requests-count="incomingRequests.length"
      :pending-incoming-count="pendingIncomingCount"
      :demo-data="demoData"
      :load-error="loadError"
      :request-dialog-visible="requestDialog"
      @update:keyword="keyword = $event"
      @search="resetFriendPage"
      @refresh="loadData"
      @open-add-friend="friendDialog = true"
      @open-friend-requests="requestDialog = true"
      @open-create-group="groupDialog = true"
    />

    <div class="page-grid contacts-page__content">
      <FriendDirectorySection
        :friends="pagedFriends"
        :loading="loading"
        :error="loadError"
        :total="filteredFriends.length"
        :pending-incoming-count="pendingIncomingCount"
        :page="friendPage"
        :page-size="friendPageSize"
        @update:page="friendPage = $event"
        @open-request-dialog="requestDialog = true"
        @open-chat="openSingleChat"
      />

      <GroupSpaceSection
        :columns="groupColumns"
        :groups="pagedGroups"
        :loading="loading"
        :error="loadError"
        :total="filteredGroups.length"
        :groups-count="groups.length"
        :keyword="groupKeyword"
        :page="groupPage"
        :page-size="groupPageSize"
        @update:keyword="groupKeyword = $event"
        @update:page="groupPage = $event"
        @search="resetGroupPage"
        @retry="loadData"
        @open-create-group="groupDialog = true"
        @open-manage="openGroupManage"
        @open-chat="openGroupChat"
      />
    </div>

    <FriendRequestDialog
      v-model="requestDialog"
      v-model:active-tab="requestTab"
      :incoming-requests="incomingRequests"
      :outgoing-requests="outgoingRequests"
      :loading="loading"
      :error="loadError"
      :action-loading="requestActionLoading"
      @retry="loadData"
      @handle-request="handleIncomingRequest"
    />

    <AddFriendDialog
      v-model="friendDialog"
      :options="addFriendUserOptions"
      :loading="userOptionsLoading"
      :error="userOptionsError"
      @submit="handleAddFriend"
    />

    <CreateGroupDialog v-model="groupDialog" @submit="handleCreateGroup" />

    <GroupManageDialog
      v-model="groupManageDialog"
      :group="groupDetail"
      :current-role="currentGroupRole"
      :members="groupMembers"
      :member-page="groupMembersPage"
      :member-page-size="groupMembersPageSize"
      :member-total="groupMembersTotal"
      :loading="groupDetailLoading"
      :members-loading="groupMembersLoading"
      :error="groupDetailError"
      :members-error="groupMembersError"
      :action-loading="groupActionLoading"
      @update:member-page="
        (page) => {
          groupMembersPage = page
          loadGroupMembersData(activeGroupId, page)
        }
      "
      @retry-detail="loadGroupDetailData"
      @retry-members="loadGroupMembersData"
      @save-group="handleSaveGroup"
      @open-invite="inviteGroupMembersDialog = true"
      @remove-member="handleRemoveGroupMember"
      @update-role="handleUpdateGroupMemberRole"
      @leave-group="handleLeaveGroup"
      @dissolve-group="handleDissolveGroup"
    />

    <InviteGroupMembersDialog
      v-model="inviteGroupMembersDialog"
      :loading="groupActionLoading === 'invite-members'"
      :options="inviteGroupUserOptions"
      :error="userOptionsError"
      @submit="handleInviteGroupMembers"
    />
  </div>
</template>

<style scoped>
.contacts-page {
  gap: 1.25rem;
}

.contacts-page__content {
  align-items: start;
}
</style>
