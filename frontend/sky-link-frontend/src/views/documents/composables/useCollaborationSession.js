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

  const editable = computed(() => ['edit', 'manage'].includes(permission.value) && status.value !== 'readonly')

  async function requestTicket() {
    const payload = await createCollaborationTicket(documentId)
    return payload?.data ?? payload
  }

  async function connect() {
    status.value = 'connecting'; error.value = ''
    firstTicket = await requestTicket()
    permission.value = firstTicket.permission
    provider.value = new HocuspocusProvider({
      url: firstTicket.websocketUrl,
      name: String(documentId),
      document: ydoc,
      token: async () => {
        const ticket = firstTicket || await requestTicket()
        firstTicket = null
        permission.value = ticket.permission
        return ticket.token
      },
    })
    provider.value.on('status', ({ status: providerStatus }) => {
      if (providerStatus === 'connected') { connectedOnce = true; status.value = 'syncing' }
      else if (connectedOnce) status.value = 'offline'
    })
    provider.value.on('synced', () => { status.value = permission.value === 'read' ? 'readonly' : 'synced' })
    provider.value.on('stateless', ({ payload }) => {
      try {
        const event = JSON.parse(payload)
        if (event.type === 'saved') { savedAt.value = event.at; status.value = permission.value === 'read' ? 'readonly' : 'saved' }
      } catch { /* ignore unknown server events */ }
    })
    provider.value.on('authenticationFailed', ({ reason }) => {
      permission.value = 'read'; status.value = 'readonly'; error.value = reason || '协同权限已失效'
    })
    ydoc.on('update', (_update, origin) => {
      if (origin !== provider.value && editable.value) status.value = provider.value?.status === 'connected' ? 'saving' : 'offline'
    })
  }

  function disconnect() { provider.value?.destroy(); provider.value = null; ydoc.destroy() }
  onBeforeUnmount(disconnect)

  return { ydoc, provider, status, permission, editable, error, savedAt, connect, disconnect }
}
