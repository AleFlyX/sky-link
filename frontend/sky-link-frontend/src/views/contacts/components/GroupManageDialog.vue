<script setup>
import { computed, reactive, watch } from 'vue'
import AppButton from '../../../components/common/AppButton.vue'
import AppCard from '../../../components/common/AppCard.vue'
import AppDataTable from '../../../components/common/AppDataTable.vue'
import AppDialog from '../../../components/common/AppDialog.vue'
import AppInput from '../../../components/common/AppInput.vue'
import AppPagination from '../../../components/common/AppPagination.vue'
import AppStatusTag from '../../../components/common/AppStatusTag.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  group: {
    type: Object,
    default: null,
  },
  currentRole: {
    type: String,
    default: '',
  },
  members: {
    type: Array,
    default: () => [],
  },
  memberPage: {
    type: Number,
    default: 1,
  },
  memberPageSize: {
    type: Number,
    default: 6,
  },
  memberTotal: {
    type: Number,
    default: 0,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  membersLoading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  membersError: {
    type: String,
    default: '',
  },
  actionLoading: {
    type: String,
    default: '',
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:memberPage',
  'retry-detail',
  'retry-members',
  'save-group',
  'open-invite',
  'remove-member',
  'update-role',
  'leave-group',
  'dissolve-group',
])

const activeTab = reactive({ value: 'info' })
const groupForm = reactive({
  groupName: '',
  notice: '',
})

const memberColumns = [
  { key: 'nickname', label: '成员' },
  { key: 'username', label: '账号' },
  { key: 'role', label: '角色', slot: 'role' },
  { key: 'joinTime', label: '加入时间' },
  { key: 'actions', label: '操作', width: '220px', align: 'center', slot: 'actions' },
]

const roleLabelMap = {
  owner: '群主',
  admin: '管理员',
  member: '普通成员',
}

const roleToneMap = {
  owner: 'primary',
  admin: 'warning',
  member: 'info',
}

const canEditGroup = computed(() => ['owner', 'admin'].includes(props.currentRole))
const canInviteMembers = computed(() => ['owner', 'admin'].includes(props.currentRole))
const canDissolveGroup = computed(() => props.currentRole === 'owner')
const canLeaveGroup = computed(
  () => props.currentRole === 'member' || props.currentRole === 'admin',
)

watch(
  () => props.group,
  (group) => {
    groupForm.groupName = group?.name || ''
    groupForm.notice = group?.notice || ''
  },
  { immediate: true },
)

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      activeTab.value = 'info'
    }
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function getRoleLabel(role) {
  return roleLabelMap[role] || role || '未知'
}

function getRoleTone(role) {
  return roleToneMap[role] || 'default'
}

function canPromote(member) {
  return props.currentRole === 'owner' && member.role === 'member'
}

function canDemote(member) {
  return props.currentRole === 'owner' && member.role === 'admin'
}

function canRemove(member) {
  if (member.role === 'owner') {
    return false
  }
  if (props.currentRole === 'owner') {
    return ['admin', 'member'].includes(member.role)
  }
  if (props.currentRole === 'admin') {
    return member.role === 'member'
  }
  return false
}

function handleSaveGroup() {
  emit('save-group', {
    groupName: groupForm.groupName,
    notice: groupForm.notice,
  })
}
</script>

<template>
  <AppDialog :model-value="modelValue" title="群管理" width="1100px" @close="closeDialog">
    <div class="group-manage-dialog">
      <el-skeleton v-if="loading && !group" :rows="8" animated />

      <template v-else>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />

        <AppCard v-if="group" variant="hero" padding="sm">
          <div class="group-manage-dialog__overview">
            <div class="group-manage-dialog__overview-main">
              <h3>{{ group.name }}</h3>
              <p>{{ group.notice || '暂无群公告' }}</p>
            </div>

            <div class="group-manage-dialog__overview-tags">
              <AppStatusTag :label="`${group.memberCount} 人`" tone="primary" />
              <AppStatusTag :label="getRoleLabel(currentRole)" :tone="getRoleTone(currentRole)" />
              <AppStatusTag :label="`群主：${group.ownerName || '-'}`" tone="info" />
            </div>
          </div>
        </AppCard>

        <el-tabs v-model="activeTab.value" class="group-manage-dialog__tabs">
          <el-tab-pane label="群信息" name="info">
            <div class="group-manage-dialog__panel">
              <AppCard title="基本信息" subtitle="查看或修改群名称和群公告">
                <div v-if="group" class="group-manage-dialog__form">
                  <label class="group-manage-dialog__field">
                    <span>群聊名称</span>
                    <AppInput v-model="groupForm.groupName" :disabled="!canEditGroup" />
                  </label>

                  <label class="group-manage-dialog__field">
                    <span>群公告</span>
                    <AppInput
                      v-model="groupForm.notice"
                      type="textarea"
                      :disabled="!canEditGroup"
                    />
                  </label>

                  <div class="group-manage-dialog__meta-list">
                    <div>
                      <strong>群主</strong>
                      <span>{{ group.ownerName || '-' }}</span>
                    </div>
                    <div>
                      <strong>成员数</strong>
                      <span>{{ group.memberCount }}</span>
                    </div>
                    <div>
                      <strong>创建时间</strong>
                      <span>{{ group.createTime || '暂无记录' }}</span>
                    </div>
                  </div>

                  <div v-if="canEditGroup" class="group-manage-dialog__actions">
                    <AppButton
                      variant="primary"
                      :loading="actionLoading === 'save-group'"
                      @click="handleSaveGroup"
                    >
                      保存修改
                    </AppButton>
                  </div>
                </div>
              </AppCard>

              <AppCard variant="ghost" padding="sm">
                <div class="group-manage-dialog__danger-actions">
                  <AppButton
                    v-if="canLeaveGroup"
                    variant="warning"
                    :loading="actionLoading === 'leave-group'"
                    @click="emit('leave-group')"
                  >
                    退出群聊
                  </AppButton>
                  <AppButton
                    v-if="canDissolveGroup"
                    variant="danger"
                    :loading="actionLoading === 'dissolve-group'"
                    @click="emit('dissolve-group')"
                  >
                    解散群组
                  </AppButton>
                </div>
              </AppCard>
            </div>
          </el-tab-pane>

          <el-tab-pane label="成员管理" name="members">
            <AppCard title="成员列表" :subtitle="`共 ${memberTotal} 人`">
              <div class="page-toolbar">
                <span class="group-manage-dialog__hint">查看群成员、角色和加入时间</span>
                <AppButton v-if="canInviteMembers" variant="primary" @click="emit('open-invite')">
                  邀请成员
                </AppButton>
              </div>

              <AppDataTable
                :columns="memberColumns"
                :rows="members"
                :loading="membersLoading"
                :error="membersError"
                empty-text="暂无成员"
                @retry="emit('retry-members')"
              >
                <template #nickname="{ row }">
                  <div class="group-manage-dialog__member">
                    <strong>{{ row.nickname || row.username || `用户#${row.userId}` }}</strong>
                    <span>ID {{ row.userId }}</span>
                  </div>
                </template>

                <template #role="{ value }">
                  <AppStatusTag :label="getRoleLabel(value)" :tone="getRoleTone(value)" />
                </template>

                <template #actions="{ row }">
                  <div class="group-manage-dialog__member-actions">
                    <AppButton
                      v-if="canPromote(row)"
                      size="small"
                      variant="primary"
                      :loading="actionLoading === `role-admin-${row.userId}`"
                      @click="emit('update-role', row, 'admin')"
                    >
                      设为管理员
                    </AppButton>
                    <AppButton
                      v-if="canDemote(row)"
                      size="small"
                      variant="warning"
                      :loading="actionLoading === `role-member-${row.userId}`"
                      @click="emit('update-role', row, 'member')"
                    >
                      取消管理员
                    </AppButton>
                    <AppButton
                      v-if="canRemove(row)"
                      size="small"
                      variant="danger"
                      :loading="actionLoading === `remove-member-${row.userId}`"
                      @click="emit('remove-member', row)"
                    >
                      移除
                    </AppButton>
                  </div>
                </template>
              </AppDataTable>

              <AppPagination
                :page="memberPage"
                :page-size="memberPageSize"
                :total="memberTotal"
                @update:page="emit('update:memberPage', $event)"
              />
            </AppCard>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </AppDialog>
</template>

<style scoped>
.group-manage-dialog {
  display: grid;
  gap: 1rem;
}

.group-manage-dialog__overview {
  display: grid;
  gap: 0.9rem;
}

.group-manage-dialog__overview-main {
  display: grid;
  gap: 0.4rem;
}

.group-manage-dialog__overview-main h3,
.group-manage-dialog__overview-main p {
  margin: 0;
}

.group-manage-dialog__overview-main p,
.group-manage-dialog__hint,
.group-manage-dialog__member span,
.group-manage-dialog__field span,
.group-manage-dialog__meta-list span {
  color: var(--color-text-muted);
}

.group-manage-dialog__overview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.group-manage-dialog__tabs {
  min-width: 0;
}

.group-manage-dialog__panel {
  display: grid;
  gap: 1rem;
}

.group-manage-dialog__form {
  display: grid;
  gap: 1rem;
}

.group-manage-dialog__field {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.group-manage-dialog__meta-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.group-manage-dialog__meta-list div {
  display: grid;
  gap: 0.3rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.76);
}

.group-manage-dialog__actions,
.group-manage-dialog__danger-actions,
.group-manage-dialog__member-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.group-manage-dialog__danger-actions {
  justify-content: flex-end;
}

.group-manage-dialog__member {
  display: grid;
  gap: 0.15rem;
}

@media (max-width: 900px) {
  .group-manage-dialog__meta-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .group-manage-dialog__danger-actions {
    justify-content: stretch;
  }

  .group-manage-dialog__danger-actions :deep(.app-button) {
    width: 100%;
  }
}
</style>
