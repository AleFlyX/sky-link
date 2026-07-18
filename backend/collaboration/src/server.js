// Hocuspocus 是基于 WebSocket 的 Yjs 协同服务框架。
import { Server } from '@hocuspocus/server'
// Database 扩展把 Hocuspocus 的文档读取/保存事件接到本项目仓库。
import { Database } from '@hocuspocus/extension-database'
// 从环境变量读取监听地址、密钥、允许来源和数据库连接配置。
import { loadConfig } from './config.js'
// 校验浏览器提供的短期协同票据，得到可信用户与文档上下文。
import { verifyTicket } from './ticket.js'
// 负责 Yjs 状态和 Markdown 镜像的 MySQL 读写。
import { CollaborationRepository } from './repository.js'
// 在连接存活期间向 Spring Boot 二次确认文档权限仍然有效。
import { reauthorize } from './authorization.js'

/**
 * 创建协同 WebSocket 服务器，但不立即监听端口，方便测试注入假配置/假仓库。
 *
 * @param {object} config 由 loadConfig 提供的运行配置。
 * @param {{ load: Function, store: Function, close: Function }} repository 文档状态仓库。
 * @returns {Server} 已注册所有协同生命周期钩子的 Hocuspocus Server。
 */
export function createCollaborationServer(config, repository) {
  // 记录每篇文档最后触发修改的用户，用于把 updated_by 写进数据库。
  const lastEditors = new Map()
  // 按用户统计当前连接数，限制单用户不能无限创建 WebSocket。
  const activeByUser = new Map()
  // 保存本次写库失败、等待重试的最新文档状态；同一文档只保留一份。
  const pendingStores = new Map()
  // 轻量内存指标，由 /metrics 输出；进程重启后自然归零。
  const metrics = { activeConnections: 0, authenticationFailures: 0, persistenceFailures: 0 }

  // Hocuspocus 的 onRequest 需要结束 HTTP 响应；返回 rejected Promise 可停止后续默认处理。
  const finishHttpRequest = (response, body) => {
    // 健康检查和指标接口都返回 JSON。
    response.writeHead(200, { 'content-type': 'application/json' })
    // 写完响应体后关闭本次 HTTP 请求。
    response.end(body)
    // 保留原框架约定：用 rejection 表示该请求已完全处理。
    return Promise.reject()
  }

  // 保存成功后广播无状态事件，让前端把“正在保存”更新为“已保存”。
  const broadcastSaved = (document) => document.broadcastStateless(JSON.stringify({ type: 'saved', at: new Date().toISOString() }))

  // 将当前完整 Yjs 状态立即写库，并传入该文档最后编辑者（可能没有）。
  const storeNow = async ({ documentName, state, document }) => {
    await repository.store(Number(documentName), state, lastEditors.get(documentName) || null)
    // 只有数据库提交成功后才告诉所有连接“已保存”。
    broadcastSaved(document)
  }

  // 为瞬时数据库错误安排指数退避重试；容量超限等明确不可重试错误不会进入定时器。
  const scheduleRetry = (item, attempt = 0) => {
    // 若同一文档已有重试项，继承已有 timer，避免同时为同一文档挂多个定时器。
    const existing = pendingStores.get(item.documentName)
    pendingStores.set(item.documentName, { ...item, attempt, timer: existing?.timer })
    // 有 timer 说明已排队；retryable=false 说明错误永久存在，两种情况均无需再次安排。
    if (existing?.timer || item.error?.retryable === false) return
    // 延迟从 250ms 开始倍增，最大 30 秒，避免故障期间高频压垮数据库。
    const delay = Math.min(30000, 250 * (2 ** attempt))
    const timer = setTimeout(async () => {
      // 定时器触发时取最新状态，避免把旧编辑覆盖掉后来的编辑。
      const latest = pendingStores.get(item.documentName)
      if (!latest) return
      // 清除 timer 标记，允许本次失败后重新安排下一次重试。
      latest.timer = null
      try {
        // 成功写库后移除脏状态记录。
        await storeNow(latest)
        pendingStores.delete(item.documentName)
      } catch (error) {
        // 失败计数用于监控，同时把最新状态与错误再次排队。
        metrics.persistenceFailures += 1
        scheduleRetry({ ...latest, error }, attempt + 1)
      }
    }, delay)
    // 回写 timer，下一次状态保存发现它后不会重复创建重试任务。
    pendingStores.get(item.documentName).timer = timer
  }

  // 配置 Hocuspocus 主服务及其从连接到保存的生命周期钩子。
  const server = new Server({
    // 网络监听配置来自环境变量，避免代码里写死部署地址。
    port: config.port,
    address: config.host,
    // 将短时间内连续 Yjs 更新合并后再保存，减少每个键盘事件都写一次数据库。
    debounce: 2000,
    maxDebounce: 10000,
    // 关闭框架默认日志，项目可改由统一日志系统采集。
    quiet: true,
    // 未认证前也限制排队数据，防止攻击者先发大量消息再认证。
    maxUnauthenticatedQueueSize: 1024 * 1024,
    maxUnauthenticatedQueueMessages: 100,
    // 单条 WebSocket 载荷不超过 1 MiB，与 beforeHandleMessage 的业务检查形成双层限制。
    websocketOptions: { maxPayload: 1024 * 1024 },

    extensions: [new Database({
      // 文档首次被打开或需要状态时，从 MySQL 读取/初始化 Yjs 二进制状态。
      fetch: ({ documentName }) => repository.load(Number(documentName)),
      // Hocuspocus 请求持久化时复制 state，防止后续内存变更影响本次待保存数据。
      store: async ({ documentName, state, document }) => {
        const item = { documentName, state: new Uint8Array(state), document }
        try {
          // 先尝试立即写库，正常情况不需要重试队列。
          await storeNow(item)
          pendingStores.delete(documentName)
        } catch (error) {
          // 失败时记录指标并排队重试，但仍把错误抛给 Hocuspocus 感知本次保存失败。
          metrics.persistenceFailures += 1
          scheduleRetry({ ...item, error })
          throw error
        }
      },
    })],

    // 连接刚建立时先做浏览器来源和全局连接数防护。
    async onConnect({ requestHeaders, instance }) {
      // 非浏览器客户端可能没有 origin；浏览器携带时必须命中白名单。
      const origin = requestHeaders.get('origin')
      if (origin && !config.allowedOrigins.has(origin)) throw new Error('origin is not allowed')
      // 全局上限保护进程内存和数据库，单用户上限在 connected 钩子中处理。
      if (instance.getConnectionsCount() >= 500) throw new Error('collaboration connection limit reached')
    },

    // Hocuspocus 收到 token 后调用：验签、验证文档绑定，并为只读用户设置连接只读标记。
    async onAuthenticate({ token, documentName, connectionConfig }) {
      try {
        // expectedDocumentId 使用本次连接的 documentName，防止跨文档复用票据。
        const context = verifyTicket(token, config.ticketSecret, documentName)
        // read 权限的连接在框架层就不可写，后续消息钩子也会再次兜底。
        connectionConfig.readOnly = context.permission === 'read'
        return context
      } catch (error) {
        // 失败计数用于监控异常 token、过期 token 或权限票据问题。
        metrics.authenticationFailures += 1
        throw error
      }
    },

    // 身份验证完成、连接真正计入在线数后执行。
    async connected({ context, connection }) {
      // 每人最多五个协同连接，避免刷新/多标签页无限叠加连接。
      const count = activeByUser.get(context.userId) || 0
      if (count >= 5) return connection.close({ code: 4429, reason: 'user connection limit reached' })
      // 更新用户维度和全局维度的在线计数，并记录标记供断开时安全回收。
      activeByUser.set(context.userId, count + 1)
      metrics.activeConnections += 1
      context.counted = true

      // 每分钟向 Spring Boot 二次授权，处理连接期间权限被撤销/降级的情况。
      context.timer = setInterval(async () => {
        try {
          const authorization = await reauthorize(config, context)
          // 后端明确拒绝时立即关连接，不继续依赖旧票据。
          if (!authorization.allowed) return connection.close({ code: 4403, reason: 'permission revoked' })
          // 允许时更新时间和权限，用户被降级为 read 后马上变成只读。
          context.lastAuthorizedAt = Date.now()
          context.permission = authorization.permission
          connection.readOnly = authorization.permission === 'read'
        } catch {
          // 授权服务短暂不可用时保留最近成功授权最多两分钟，超过后为安全起见断开。
          if (Date.now() - context.lastAuthorizedAt > 120000) {
            connection.close({ code: 4503, reason: 'authorization unavailable' })
          }
        }
      }, 60000)
    },

    // 每条 Yjs 更新进入处理前都要检查大小和只读状态。
    async beforeHandleMessage({ context, connection, update }) {
      // 防止单条协同更新超过 1 MiB，即使底层 WebSocket 配置变化也保持业务限制。
      if (update?.byteLength > 1024 * 1024) throw new Error('collaboration message exceeds 1 MiB')
      // 权限二次检查刚刚可能将用户降级；这里立即强制连接只读。
      if (context.permission === 'read') connection.readOnly = true
    },

    // 文档发生变更时记住操作者；真正保存会在 debounce 后发生。
    async onChange({ context, documentName }) {
      if (context?.userId) lastEditors.set(documentName, context.userId)
    },

    // 连接断开时停止授权定时器，并回收在线计数。
    async onDisconnect({ context }) {
      // 未成功认证的连接可能没有 context/timer，使用可选链安全处理。
      if (context?.timer) clearInterval(context.timer)
      if (context?.counted) {
        // 计数归零时删除 Map 项，避免长期积累离线用户键。
        const count = activeByUser.get(context.userId) || 1
        if (count <= 1) activeByUser.delete(context.userId)
        else activeByUser.set(context.userId, count - 1)
        metrics.activeConnections -= 1
      }
    },

    // Hocuspocus 也可响应普通 HTTP 请求，供运维健康检查和指标采集。
    async onRequest({ request, response }) {
      // 健康检查只表示进程可响应；不代表数据库或 Spring 授权服务一定可用。
      if (request.url === '/health') return finishHttpRequest(response, '{"status":"ok"}')
      // 暴露非敏感内存指标，以及当前等待持久化重试的文档数量。
      if (request.url === '/metrics') {
        return finishHttpRequest(response, JSON.stringify({ ...metrics, dirtyDocuments: pendingStores.size }))
      }
    },
  })

  // 测试或外部监控可直接读取同一个指标对象，无需触碰闭包内部 Map。
  server.metrics = metrics
  return server
}

// 测试导入本模块时不应自动启动监听端口；真实运行才加载环境配置并启动服务。
if (process.env.NODE_ENV !== 'test') {
  // 创建配置、数据库仓库和已注册钩子的服务器。
  const config = loadConfig()
  const repository = CollaborationRepository.create(config.database)
  const server = createCollaborationServer(config, repository)
  // 顶层 await 让启动失败直接让进程退出，而不是留下半启动服务。
  await server.listen()

  // 收到 Ctrl+C 或容器停止信号时按顺序关闭 WebSocket 服务、数据库池，再退出进程。
  const shutdown = async () => {
    await server.destroy()
    await repository.close()
    process.exit(0)
  }
  // 本地终端 Ctrl+C 通常发送 SIGINT。
  process.on('SIGINT', shutdown)
  // Docker/Kubernetes 停止容器通常发送 SIGTERM。
  process.on('SIGTERM', shutdown)
}
