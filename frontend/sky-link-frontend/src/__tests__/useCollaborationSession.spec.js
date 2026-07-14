import { effectScope } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const providers = []
vi.mock('@hocuspocus/provider', () => ({
  HocuspocusProvider: class {
    constructor(configuration) { this.configuration = configuration; this.handlers = {}; this.status = 'disconnected'; providers.push(this) }
    on(name, callback) { this.handlers[name] = callback }
    emit(name, payload = {}) { this.handlers[name]?.(payload) }
    destroy() {}
  },
}))
vi.mock('../api/document', () => ({
  createCollaborationTicket: vi.fn(async () => ({ data: { token: 'ticket', websocketUrl: 'ws://localhost:1234', permission: 'edit' } })),
}))

import { useCollaborationSession } from '../views/documents/composables/useCollaborationSession'

describe('collaborative editor connection states', () => {
  beforeEach(() => { providers.length = 0 })
  it('moves from connecting to synced and persisted', async () => {
    const scope = effectScope(); const session = scope.run(() => useCollaborationSession(9))
    await session.connect(); const provider = providers[0]
    provider.status = 'connected'; provider.emit('status', { status: 'connected' }); provider.emit('synced')
    expect(session.status.value).toBe('synced')
    provider.emit('stateless', { payload: JSON.stringify({ type: 'saved', at: '2026-07-14T12:00:00Z' }) })
    expect(session.status.value).toBe('saved'); expect(session.savedAt.value).toBe('2026-07-14T12:00:00Z')
    scope.stop()
  })

  it('switches to read-only when authentication is revoked', async () => {
    const scope = effectScope(); const session = scope.run(() => useCollaborationSession(9))
    await session.connect(); providers[0].emit('authenticationFailed', { reason: 'permission revoked' })
    expect(session.status.value).toBe('readonly'); expect(session.editable.value).toBe(false)
    expect(session.error.value).toBe('permission revoked'); scope.stop()
  })
})
