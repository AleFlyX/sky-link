import { createHmac } from 'node:crypto'
import { HocuspocusProvider } from '@hocuspocus/provider'
import { afterEach, describe, expect, it } from 'vitest'
import WebSocket from 'ws'
import * as Y from 'yjs'
import { createCollaborationServer } from '../src/server.js'

const secret = '01234567890123456789012345678901'
const servers = []
const providers = []
function ticket(documentId, userId, permission) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url')
  const payload = Buffer.from(JSON.stringify({ iss: 'sky-link', aud: 'sky-link-collaboration', sub: String(userId), documentId,
    permission, displayName: `User ${userId}`, jti: `${userId}`, iat: 1, exp: Math.floor(Date.now() / 1000) + 60 })).toString('base64url')
  const unsigned = `${header}.${payload}`
  return `${unsigned}.${createHmac('sha256', secret).update(unsigned).digest('base64url')}`
}
const waitFor = (provider, event) => new Promise((resolve, reject) => {
  const timeout = setTimeout(() => reject(new Error(`timed out waiting for ${event}`)), 5000)
  provider.on(event, (payload) => { clearTimeout(timeout); resolve(payload) })
})
afterEach(async () => {
  providers.splice(0).forEach((provider) => provider.destroy())
  for (const server of servers.splice(0)) await server.destroy()
})

describe('Hocuspocus websocket boundary', () => {
  it('synchronizes editors, persists updates, and blocks a read-only writer', async () => {
    const stored = []; let failuresRemaining = 1
    const repository = { load: async () => null, store: async (_id, state) => {
      if (failuresRemaining-- > 0) throw new Error('temporary database outage')
      stored.push(new Uint8Array(state))
    } }
    const port = 13241
    const server = createCollaborationServer({ port, host: '127.0.0.1', allowedOrigins: new Set(), ticketSecret: secret,
      serviceToken: 'service-token-at-least-32-characters', internalBaseUrl: 'http://127.0.0.1:1' }, repository)
    servers.push(server); await server.listen()
    const writerDoc = new Y.Doc(); const readerDoc = new Y.Doc()
    const writer = new HocuspocusProvider({ url: `ws://127.0.0.1:${port}`, name: '42', document: writerDoc,
      token: ticket(42, 1, 'edit'), WebSocketPolyfill: WebSocket })
    const reader = new HocuspocusProvider({ url: `ws://127.0.0.1:${port}`, name: '42', document: readerDoc,
      token: ticket(42, 2, 'read'), WebSocketPolyfill: WebSocket })
    providers.push(writer, reader); await Promise.all([waitFor(writer, 'synced'), waitFor(reader, 'synced')])
    writerDoc.getText('note').insert(0, 'approved')
    await new Promise((resolve) => setTimeout(resolve, 250))
    expect(readerDoc.getText('note').toString()).toBe('approved')
    readerDoc.getText('note').insert(0, 'forged-')
    await new Promise((resolve) => setTimeout(resolve, 250))
    expect(writerDoc.getText('note').toString()).toBe('approved')
    await new Promise((resolve) => setTimeout(resolve, 2200))
    expect(stored.length).toBeGreaterThan(0)
    expect(server.metrics.persistenceFailures).toBeGreaterThan(0)
  }, 10000)
})
