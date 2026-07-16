<script setup>
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import { Image } from '@tiptap/extension-image'
import { Table, TableCell, TableHeader, TableRow } from '@tiptap/extension-table'
import StarterKit from '@tiptap/starter-kit'
import { Editor } from '@tiptap/core'
import { EditorContent } from '@tiptap/vue-3'
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import AppCard from '../../components/common/AppCard.vue'
import AppInput from '../../components/common/AppInput.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { getDocument, updateDocument } from '../../api/document'
import { useUserStore } from '../../stores/user'
import { useCollaborationSession } from './composables/useCollaborationSession'

import { visibilityOptions } from './constant/enum.js'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const documentId = Number(route.params.documentId)
const document = ref(null)
const loading = ref(true)
const titleSaving = ref(false)
const statusSaving = ref(false)
const session = useCollaborationSession(documentId)
const colors = ['#0066CC', '#009966', '#CC6600', '#663399', '#CC3333']
const userColor = colors[Math.abs(Number(userStore.user?.id) || 0) % colors.length]

const visibilityLabels = Object.fromEntries(
  visibilityOptions.map((option) => [option.value, option.label]),
)
const adminRoleCodes = new Set(['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])
const previousStatus = ref('private')
const visibilityDialog = ref(false)
const visibilityDraft = ref('private')
const canEditContent = computed(() => Boolean(editor.value && session.editable.value))
const canManageDocument = computed(() => {
  const currentUserId = Number(userStore.user?.id)
  const creatorId = Number(document.value?.creatorId)
  const roles = Array.isArray(userStore.user?.roles) ? userStore.user.roles : []
  const hasAdminRole = roles.some((role) => {
    if (!role) return false
    const code = String(
      role.roleCode || role.code || role.name || role.roleName || role.label || '',
    )
      .trim()
      .toUpperCase()
    return adminRoleCodes.has(code)
  })

  return (
    session.permission.value === 'manage' ||
    (Number.isFinite(currentUserId) && currentUserId === creatorId) ||
    hasAdminRole
  )
})
const currentVisibilityLabel = computed(
  () => visibilityLabels[document.value?.status] || visibilityLabels.private,
)

const editor = shallowRef(null)

watch(session.editable, (value) => editor.value?.setEditable(value), { immediate: true })
const statusCopy = computed(
  () =>
    ({
      connecting: '正在连接',
      syncing: '正在同步',
      synced: '已同步',
      saving: '保存中',
      saved: '已保存',
      offline: '离线编辑中',
      readonly: '只读',
      error: '同步失败',
    })[session.status.value] || '正在连接',
)

async function load() {
  try {
    const payload = await getDocument(documentId)
    document.value = payload?.data ?? payload
    previousStatus.value = document.value?.status || 'private'
    await session.connect()
    editor.value = new Editor({
      editable: session.editable.value,
      extensions: [
        StarterKit.configure({ undoRedo: false }),
        Image.configure({ allowBase64: true, inline: false }),
        Table.configure({ resizable: true }),
        TableRow,
        TableHeader,
        TableCell,
        Collaboration.configure({ document: session.ydoc, field: 'default' }),
        CollaborationCaret.configure({
          provider: session.provider.value,
          user: { name: userStore.displayName || '协作者', color: userColor },
        }),
      ],
    })
  } catch (error) {
    session.error.value = error.message
    session.status.value = 'error'
  } finally {
    loading.value = false
  }
}
async function saveTitle() {
  if (!document.value?.title?.trim()) return ElMessage.warning('文档标题不能为空')
  titleSaving.value = true
  try {
    await updateDocument(documentId, { title: document.value.title.trim() })
    ElMessage.success('标题已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    titleSaving.value = false
  }
}
function openVisibilityDialog() {
  if (!document.value) return
  visibilityDraft.value = document.value.status || 'private'
  visibilityDialog.value = true
}
async function saveVisibility() {
  if (!document.value) return
  const nextStatus = visibilityDraft.value
  if (!nextStatus || nextStatus === previousStatus.value) {
    visibilityDialog.value = false
    return
  }
  statusSaving.value = true
  try {
    await updateDocument(documentId, { status: nextStatus })
    previousStatus.value = nextStatus
    document.value.status = nextStatus
    session.permission.value = nextStatus === 'archived' ? 'read' : 'manage'
    session.status.value = nextStatus === 'archived' ? 'readonly' : 'synced'
    editor.value?.setEditable(nextStatus !== 'archived')
    visibilityDialog.value = false
    ElMessage.success('可见范围已保存')
  } catch (error) {
    visibilityDraft.value = previousStatus.value
    ElMessage.error(error.message)
  } finally {
    statusSaving.value = false
  }
}
async function copyContent() {
  await navigator.clipboard.writeText(editor.value?.getText() || '')
  ElMessage.success('当前内容已复制')
}

function insertTable() {
  if (!canEditContent.value) return
  editor.value.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

function removeTable() {
  if (!canEditContent.value) return
  editor.value.chain().focus().deleteTable().run()
}

function insertImage() {
  if (!canEditContent.value) return
  const src = window.prompt('请输入图片地址（URL）')
  const normalizedSrc = src?.trim()
  if (!normalizedSrc) return
  editor.value.chain().focus().setImage({ src: normalizedSrc, alt: '插入图片' }).run()
}

onMounted(load)
onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="collaboration-page">
    <AppCard
      variant="hero"
      padding="lg"
      class="collaboration-header-card"
      body-class="collaboration-header-card__body"
    >
      <div class="collaboration-header">
        <div class="collaboration-header__nav">
          <AppButton variant="secondary" @click="router.push('/app/documents')">返回文档</AppButton>
          <div
            class="sync-status sync-status--compact"
            :class="`sync-status--${session.status.value}`"
          >
            <span>{{ statusCopy }}</span>
            <small v-if="session.savedAt.value">{{
              new Date(session.savedAt.value).toLocaleTimeString()
            }}</small>
          </div>
        </div>
        <div class="document-meta">
          <div class="document-meta__title-row">
            <AppInput
              v-if="document"
              v-model="document.title"
              class="document-title-input"
              :disabled="!canEditContent || titleSaving || statusSaving"
              @blur="saveTitle"
            />
            <div class="document-actions">
              <AppStatusTag
                v-if="document"
                :label="currentVisibilityLabel"
                :tone="document.status === 'archived' ? 'info' : 'primary'"
              />
              <AppButton
                v-if="canManageDocument"
                size="small"
                variant="secondary"
                @click="openVisibilityDialog"
                >修改可见范围</AppButton
              >
            </div>
          </div>
          <p class="document-meta__hint">
            标题、可见范围和协同内容彼此分离，方便单独调整文档状态。
          </p>
        </div>
      </div>
    </AppCard>
    <el-alert
      v-if="session.error.value"
      :title="session.error.value"
      type="warning"
      show-icon
      :closable="false"
    >
      <template #default
        ><AppButton variant="secondary" @click="copyContent">复制当前内容</AppButton></template
      >
    </el-alert>
    <el-skeleton v-if="loading" :rows="12" animated />
    <main v-else class="editor-paper" :aria-busy="session.status.value === 'connecting'">
      <div class="editor-toolbar">
        <AppButton size="small" variant="secondary" :disabled="!canEditContent" @click="insertTable"
          >插入表格</AppButton
        >
        <AppButton
          size="small"
          variant="secondary"
          :disabled="!canEditContent || !editor?.isActive('table')"
          @click="removeTable"
          >删除表格</AppButton
        >
        <AppButton size="small" variant="secondary" :disabled="!canEditContent" @click="insertImage"
          >插入图片</AppButton
        >
      </div>
      <EditorContent :editor="editor" />
      <p class="editor-hint">图片通过 URL 插入，表格会作为协同文档节点保存。</p>
      <div v-if="!session.editable.value" class="readonly-hint">当前文档为只读状态</div>
    </main>
    <el-dialog v-model="visibilityDialog" title="修改可见范围" width="min(480px, 92vw)">
      <div class="visibility-dialog">
        <p class="visibility-dialog__hint">
          修改可见范围会影响成员是否可以直接查看该文档；归档后会暂时转为只读，恢复后可继续编辑。
        </p>
        <el-select
          v-model="visibilityDraft"
          class="visibility-dialog__select"
          :disabled="statusSaving"
        >
          <el-option
            v-for="option in visibilityOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
      <template #footer>
        <div class="visibility-dialog__footer">
          <AppButton :disabled="statusSaving" @click="visibilityDialog = false">取消</AppButton>
          <AppButton variant="primary" :disabled="statusSaving" @click="saveVisibility"
            >保存</AppButton
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.collaboration-page {
  display: grid;
  gap: 1rem;
}
.collaboration-header-card__body {
  padding-bottom: 1rem;
}
.collaboration-header {
  display: grid;
  gap: 1rem;
}
.collaboration-header__nav {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
}
.document-meta {
  display: grid;
  gap: 0.5rem;
  flex: 1;
  min-width: min(100%, 32rem);
}
.document-meta__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.document-title-input {
  flex: 1;
  min-width: min(100%, 28rem);
  max-width: 48rem;
}
.document-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.document-meta__hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.7;
}
.sync-status {
  display: flex;
  gap: 0.5rem;
  align-items: baseline;
  color: var(--color-text-muted);
}
.sync-status--compact {
  padding-top: 0.25rem;
  text-align: right;
}
.sync-status--saved,
.sync-status--synced {
  color: #087f5b;
}
.sync-status--offline,
.sync-status--error {
  color: #c2410c;
}
.editor-paper {
  position: relative;
  max-width: 980px;
  width: 100%;
  min-height: 70vh;
  margin: 0 auto;
  padding: 2rem 2.25rem 3rem;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  box-shadow: 0 18px 50px rgb(15 23 42 / 8%);
}
.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding-bottom: 1rem;
  margin-bottom: 1.25rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
}
.editor-paper :deep(.tiptap) {
  min-height: 60vh;
  outline: none;
  line-height: 1.75;
}
.editor-paper :deep(.tiptap img) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgb(15 23 42 / 10%);
}
.editor-paper :deep(.tiptap table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
  overflow: hidden;
}
.editor-paper :deep(.tiptap th),
.editor-paper :deep(.tiptap td) {
  border: 1px solid var(--color-border);
  padding: 0.65rem 0.8rem;
  vertical-align: top;
}
.editor-paper :deep(.tiptap th) {
  background: #f8fafc;
  font-weight: 600;
}
.editor-hint {
  margin: 0.8rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}
.readonly-hint {
  position: absolute;
  right: 1rem;
  bottom: 1rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
.visibility-dialog {
  display: grid;
  gap: 1rem;
}
.visibility-dialog__hint {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}
.visibility-dialog__select {
  width: 100%;
}
.visibility-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
@media (max-width: 720px) {
  .editor-paper {
    padding: 1.5rem;
  }
  .document-title-input {
    min-width: 0;
    width: 100%;
  }
  .document-actions {
    justify-content: flex-start;
  }
  .sync-status--compact {
    text-align: left;
  }
}
</style>
