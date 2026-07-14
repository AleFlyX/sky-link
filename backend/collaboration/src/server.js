import { Server } from '@hocuspocus/server'
import { Database } from '@hocuspocus/extension-database'
import { loadConfig } from './config.js'
import { verifyTicket } from './ticket.js'
import { CollaborationRepository } from './repository.js'
import { reauthorize } from './authorization.js'

export function createCollaborationServer(config, repository) {
  const lastEditors = new Map()
  const activeByUser = new Map(); const pendingStores = new Map()
  const metrics = { activeConnections: 0, authenticationFailures: 0, persistenceFailures: 0 }
  const broadcastSaved = (document) => document.broadcastStateless(JSON.stringify({ type: 'saved', at: new Date().toISOString() }))
  const storeNow = async ({ documentName, state, document }) => {
    await repository.store(Number(documentName), state, lastEditors.get(documentName) || null); broadcastSaved(document)
  }
  const scheduleRetry = (item, attempt = 0) => {
    const existing = pendingStores.get(item.documentName)
    pendingStores.set(item.documentName, { ...item, attempt, timer: existing?.timer })
    if (existing?.timer || item.error?.retryable === false) return
    const delay = Math.min(30000, 250 * (2 ** attempt))
    const timer = setTimeout(async () => {
      const latest = pendingStores.get(item.documentName); if (!latest) return
      latest.timer = null
      try { await storeNow(latest); pendingStores.delete(item.documentName) }
      catch (error) { metrics.persistenceFailures += 1; scheduleRetry({ ...latest, error }, attempt + 1) }
    }, delay)
    pendingStores.get(item.documentName).timer = timer
  }
  const server = new Server({
    port: config.port, address: config.host, debounce: 2000, maxDebounce: 10000, quiet: true,
    maxUnauthenticatedQueueSize: 1024 * 1024, maxUnauthenticatedQueueMessages: 100,
    websocketOptions: { maxPayload: 1024 * 1024 },
    extensions: [new Database({
      fetch: ({ documentName }) => repository.load(Number(documentName)),
      store: async ({ documentName, state, document }) => {
        const item = { documentName, state: new Uint8Array(state), document }
        try { await storeNow(item); pendingStores.delete(documentName) }
        catch (error) { metrics.persistenceFailures += 1; scheduleRetry({ ...item, error }); throw error }
      },
    })],
    async onConnect({ requestHeaders, instance }) {
      const origin = requestHeaders.get('origin')
      if (origin && !config.allowedOrigins.has(origin)) throw new Error('origin is not allowed')
      if (instance.getConnectionsCount() >= 500) throw new Error('collaboration connection limit reached')
    },
    async onAuthenticate({ token, documentName, connectionConfig }) {
      try {
        const context = verifyTicket(token, config.ticketSecret, documentName)
        connectionConfig.readOnly = context.permission === 'read'; return context
      } catch (error) { metrics.authenticationFailures += 1; throw error }
    },
    async connected({ context, connection }) {
      const count = activeByUser.get(context.userId) || 0
      if (count >= 5) return connection.close({ code: 4429, reason: 'user connection limit reached' })
      activeByUser.set(context.userId, count + 1); metrics.activeConnections += 1; context.counted = true
      context.timer = setInterval(async () => {
        try {
          const authorization = await reauthorize(config, context)
          if (!authorization.allowed) return connection.close({ code: 4403, reason: 'permission revoked' })
          context.lastAuthorizedAt = Date.now(); context.permission = authorization.permission
          connection.readOnly = authorization.permission === 'read'
        } catch {
          if (Date.now() - context.lastAuthorizedAt > 120000) connection.close({ code: 4503, reason: 'authorization unavailable' })
        }
      }, 60000)
    },
    async beforeHandleMessage({ context, connection, update }) {
      if (update?.byteLength > 1024 * 1024) throw new Error('collaboration message exceeds 1 MiB')
      if (context.permission === 'read') connection.readOnly = true
    },
    async onChange({ context, documentName }) { if (context?.userId) lastEditors.set(documentName, context.userId) },
    async onDisconnect({ context }) {
      if (context?.timer) clearInterval(context.timer)
      if (context?.counted) { const count = activeByUser.get(context.userId) || 1; count <= 1 ? activeByUser.delete(context.userId) : activeByUser.set(context.userId, count - 1); metrics.activeConnections -= 1 }
    },
    async onRequest({ request, response }) {
      if (request.url === '/health') { response.writeHead(200, { 'content-type': 'application/json' }); response.end('{"status":"ok"}') }
      if (request.url === '/metrics') { response.writeHead(200, { 'content-type': 'application/json' }); response.end(JSON.stringify({ ...metrics, dirtyDocuments: pendingStores.size })) }
    },
  })
  server.metrics = metrics
  return server
}

if (process.env.NODE_ENV !== 'test') {
  const config = loadConfig(); const repository = CollaborationRepository.create(config.database)
  const server = createCollaborationServer(config, repository)
  await server.listen()
  const shutdown = async () => { await server.destroy(); await repository.close(); process.exit(0) }
  process.on('SIGINT', shutdown); process.on('SIGTERM', shutdown)
}
