<script setup>
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import StarterKit from '@tiptap/starter-kit'
import { Editor } from '@tiptap/core'
import { EditorContent } from '@tiptap/vue-3'
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import { getDocument, updateDocument } from '../../api/document'
import { useUserStore } from '../../stores/user'
import { useCollaborationSession } from './composables/useCollaborationSession'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const documentId = Number(route.params.documentId)
const document = ref(null); const loading = ref(true); const titleSaving = ref(false)
const session = useCollaborationSession(documentId)
const colors = ['#0066CC', '#009966', '#CC6600', '#663399', '#CC3333']
const userColor = colors[Math.abs(Number(userStore.user?.id) || 0) % colors.length]

const editor = shallowRef(null)

watch(session.editable, (value) => editor.value?.setEditable(value), { immediate: true })
const statusCopy = computed(() => ({ connecting: '正在连接', syncing: '正在同步', synced: '已同步', saving: '保存中', saved: '已保存',
  offline: '离线编辑中', readonly: '只读', error: '同步失败' })[session.status.value] || '正在连接')

async function load() {
  try {
    const payload = await getDocument(documentId); document.value = payload?.data ?? payload
    await session.connect()
    editor.value = new Editor({ editable: session.editable.value, extensions: [
      StarterKit.configure({ undoRedo: false }),
      Collaboration.configure({ document: session.ydoc, field: 'default' }),
      CollaborationCaret.configure({ provider: session.provider.value, user: { name: userStore.displayName || '协作者', color: userColor } }),
    ] })
  } catch (error) { session.error.value = error.message; session.status.value = 'error' }
  finally { loading.value = false }
}
async function saveTitle() {
  if (!document.value?.title?.trim()) return ElMessage.warning('文档标题不能为空')
  titleSaving.value = true
  try { await updateDocument(documentId, { title: document.value.title.trim() }); ElMessage.success('标题已保存') }
  catch (error) { ElMessage.error(error.message) }
  finally { titleSaving.value = false }
}
async function copyContent() {
  await navigator.clipboard.writeText(editor.value?.getText() || '')
  ElMessage.success('当前内容已复制')
}
onMounted(load)
onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="collaboration-page">
    <header class="collaboration-header">
      <AppButton variant="secondary" @click="router.push('/app/documents')">返回文档</AppButton>
      <el-input v-if="document" v-model="document.title" class="document-title-input" :disabled="session.permission.value === 'read'" @blur="saveTitle" />
      <div class="sync-status" :class="`sync-status--${session.status.value}`">
        <span>{{ statusCopy }}</span>
        <small v-if="session.savedAt.value">{{ new Date(session.savedAt.value).toLocaleTimeString() }}</small>
      </div>
    </header>
    <el-alert v-if="session.error.value" :title="session.error.value" type="warning" show-icon :closable="false">
      <template #default><AppButton variant="secondary" @click="copyContent">复制当前内容</AppButton></template>
    </el-alert>
    <el-skeleton v-if="loading" :rows="12" animated />
    <main v-else class="editor-paper" :aria-busy="session.status.value === 'connecting'">
      <EditorContent :editor="editor" />
      <div v-if="!session.editable.value" class="readonly-hint">当前文档为只读状态</div>
    </main>
  </div>
</template>

<style scoped>
.collaboration-page{display:grid;gap:1rem}.collaboration-header{display:grid;grid-template-columns:auto minmax(16rem,1fr) auto;align-items:center;gap:1rem}
.document-title-input{max-width:48rem}.sync-status{display:flex;gap:.5rem;align-items:baseline;color:var(--color-text-muted)}
.sync-status--saved,.sync-status--synced{color:#087f5b}.sync-status--offline,.sync-status--error{color:#c2410c}
.editor-paper{position:relative;max-width:980px;width:100%;min-height:70vh;margin:0 auto;padding:3rem 4rem;background:#fff;border:1px solid var(--color-border);border-radius:18px;box-shadow:0 18px 50px rgb(15 23 42 / 8%)}
.editor-paper :deep(.tiptap){min-height:60vh;outline:none;line-height:1.75}.readonly-hint{position:absolute;right:1rem;bottom:1rem;color:var(--color-text-muted);font-size:.85rem}
@media(max-width:720px){.collaboration-header{grid-template-columns:1fr}.editor-paper{padding:1.5rem}}
</style>
