import { HocuspocusProvider } from '@hocuspocus/provider'
import { computed, onBeforeUnmount, ref } from 'vue'
import * as Y from 'yjs'
import { createCollaborationTicket } from '../../../api/document'

export function useCollaborationSession(documentId) {
  const ydoc = new Y.Doc()
  const provider = ref(null)
  const status = ref('connecting')
  const permission = ref('read')
  const error = ref('')
  const savedAt = ref(null)
  let connectedOnce = false
  let firstTicket = null

  const editable = computed(
    // 前端据后端签发的协同票据决定编辑体验；服务器仍会在协同连接上再次执行授权。
    () => ['edit', 'manage'].includes(permission.value) && status.value !== 'readonly',
  )

  async function requestTicket() {
    // 每次建立/续接协同连接前向后端换取短期 ticket，不直接暴露长期认证信息给协同服务。
    const payload = await createCollaborationTicket(documentId)
    return payload?.data ?? payload
  }

  async function connect() {
    status.value = 'connecting'
    error.value = ''
    // 首张票据同时提供 WebSocket 地址和当前权限；后续 provider 需要 token 时再按需取票。
    firstTicket = await requestTicket()
    permission.value = firstTicket.permission
    provider.value = new HocuspocusProvider({
      url: firstTicket.websocketUrl,
      name: String(documentId),
      document: ydoc,
      token: async () => {
        // firstTicket 只消费一次，随后重新申请，避免重复使用已消费或即将过期的票据。
        const ticket = firstTicket || (await requestTicket())
        firstTicket = null
        permission.value = ticket.permission
        return ticket.token
      },
    })
    provider.value.on('status', ({ status: providerStatus }) => {
      if (providerStatus === 'connected') {
        connectedOnce = true
        status.value = 'syncing'
      } else if (connectedOnce) status.value = 'offline'
    })
    provider.value.on('synced', () => {
      // Yjs 首次同步完成才表示本地文档已与协同服务对齐；只读用户保持 readonly 状态。
      status.value = permission.value === 'read' ? 'readonly' : 'synced'
    })
    provider.value.on('stateless', ({ payload }) => {
      try {
        const event = JSON.parse(payload)
        if (event.type === 'saved') {
          savedAt.value = event.at
          status.value = permission.value === 'read' ? 'readonly' : 'saved'
        }
      } catch {
        /* ignore unknown server events */
      }
    })
    provider.value.on('authenticationFailed', ({ reason }) => {
      // 协同服务拒绝认证时主动降为只读，防止页面继续给用户“可以编辑”的错误印象。
      permission.value = 'read'
      status.value = 'readonly'
      error.value = reason || '协同权限已失效'
    })
    ydoc.on('update', (_update, origin) => {
      if (origin !== provider.value && editable.value)
        status.value = provider.value?.status === 'connected' ? 'saving' : 'offline'
    })
  }

  function disconnect() {
    // 组件离开时销毁 provider 和 Yjs 文档，避免旧连接、监听器和内存继续存活。
    provider.value?.destroy()
    provider.value = null
    ydoc.destroy()
  }
  onBeforeUnmount(disconnect)

  return { ydoc, provider, status, permission, editable, error, savedAt, connect, disconnect }
}
