// mysql2/promise 提供基于 Promise 的 MySQL 连接池 API，避免在协同保存链路中使用回调。
import mysql from 'mysql2/promise'
// Yjs 负责把多人编辑状态编码为可合并的二进制更新。
import * as Y from 'yjs'
// 文档首次进入协同时，需要在普通 Markdown 内容与 Yjs 文档之间转换。
import { markdownToYDoc, yDocToMarkdown } from './markdown.js'

/**
 * 协同文档的数据库仓库。
 *
 * 它同时维护两份一致的数据：`document_collaboration_state` 中的 Yjs 二进制状态，
 * 以及 `document.content` 中供普通文档页面/检索使用的 Markdown 文本。
 */
export class CollaborationRepository {
  /**
   * @param {mysql.Pool} pool 已创建的 MySQL 连接池。
   */
  constructor(pool) {
    // 所有 load/store 都复用此连接池，而不是每个 WebSocket 保存动作单独建立连接。
    this.pool = pool
  }

  /**
   * 用数据库配置创建仓库及连接池。
   *
   * @param {object} config 来自 loadConfig().database 的 mysql2 配置。
   * @returns {CollaborationRepository} 可供 Hocuspocus Database 扩展使用的仓库。
   */
  static create(config) {
    // createPool 不会立即耗尽连接；连接会在首次执行 SQL 时按需建立。
    return new CollaborationRepository(mysql.createPool(config))
  }

  /**
   * 读取文档当前 Yjs 状态；首次协作时将原 Markdown 初始化成 Yjs 状态。
   *
   * @param {string|number} documentId Hocuspocus 的 documentName，对应项目 document_id。
   * @returns {Promise<Uint8Array>} 可直接交给 Hocuspocus/Yjs 的状态更新二进制数据。
   */
  async load(documentId) {
    // 优先读取已持久化的协同状态；参数化 SQL 防止 documentId 被拼接进语句。
    const [states] = await this.pool.execute(
      'SELECT ydoc_state FROM document_collaboration_state WHERE document_id = ?',
      [documentId],
    )
    // 查到状态时直接还原为 Uint8Array，避免再做 Markdown 往返转换而丢失协同细节。
    if (states.length) return new Uint8Array(states[0].ydoc_state)

    // 首次协作还没有状态记录：从主文档表读取当前 Markdown；逻辑删除文档不可协作。
    const [documents] = await this.pool.execute(
      'SELECT content FROM document WHERE document_id = ? AND is_deleted = 0',
      [documentId],
    )
    // 不存在的文档不能被 Hocuspocus 自动创建为一篇空白协作文档。
    if (!documents.length) throw new Error('document not found')

    // 将已有 Markdown 转为初始 ProseMirror/Yjs 文档，空内容按空字符串处理。
    const ydoc = markdownToYDoc(documents[0].content || '')
    // Yjs 将完整当前状态编码成可传输、可持久化的 update。
    const state = Y.encodeStateAsUpdate(ydoc)
    // 将初始协同状态与对应 Markdown 一并写入数据库；首次初始化没有具体编辑者，所以 updatedBy 为 null。
    await this.store(documentId, state, null)
    // 返回新建状态给本次刚建立的协同连接。
    return state
  }

  /**
   * 原子保存最新 Yjs 状态，并同步更新普通文档表中的 Markdown 镜像。
   *
   * @param {string|number} documentId 要保存的文档 ID。
   * @param {Uint8Array|Buffer} state Hocuspocus 传入的完整 Yjs 状态。
   * @param {number|null} updatedBy 最后触发变更的用户 ID；初始化时可以为空。
   */
  async store(documentId, state, updatedBy) {
    // 限制单篇协作文档状态最大 10 MiB，防止异常客户端占满内存和数据库。
    if (state.byteLength > 10 * 1024 * 1024) {
      const error = new Error('document exceeds the 10 MiB collaboration limit')
      // server.js 据此识别“永远不该重试”的容量错误。
      error.retryable = false
      throw error
    }

    // 先把二进制状态应用到临时 Y.Doc，才能得到同一版本的状态向量和 Markdown 文本。
    const ydoc = new Y.Doc()
    Y.applyUpdate(ydoc, new Uint8Array(state))
    // 状态向量帮助记录版本；Markdown 镜像让非协同 API 仍可读取文档正文。
    const vector = Y.encodeStateVector(ydoc)
    const markdown = yDocToMarkdown(ydoc)
    // 一个连接完成两张表的更新，随后用事务保证它们不会出现版本不同步。
    const connection = await this.pool.getConnection()

    try {
      // 开始事务：下面任一 SQL 失败时，两张表都应恢复原状。
      await connection.beginTransaction()
      // 有状态记录时更新并让 revision 加 1；首次保存时插入 revision=1。
      await connection.execute(
        `INSERT INTO document_collaboration_state
          (document_id, ydoc_state, state_vector, revision, updated_by) VALUES (?, ?, ?, 1, ?)
          ON DUPLICATE KEY UPDATE ydoc_state=VALUES(ydoc_state), state_vector=VALUES(state_vector),
          revision=revision+1, updated_by=VALUES(updated_by)`,
        [documentId, Buffer.from(state), Buffer.from(vector), updatedBy],
      )
      // 使用同一次转换得到的 Markdown 更新主文档，同时跳过已经逻辑删除的记录。
      await connection.execute(
        'UPDATE document SET content = ?, update_time = CURRENT_TIMESTAMP WHERE document_id = ? AND is_deleted = 0',
        [markdown, documentId],
      )
      // 两条 SQL 都成功后才提交，使协同状态与普通文档内容对外同时可见。
      await connection.commit()
    } catch (error) {
      // 任何异常都撤销尚未提交的修改；错误继续抛给上层决定是否重试。
      await connection.rollback()
      throw error
    } finally {
      // 无论成功失败都归还连接，否则连接池最终会被耗尽。
      connection.release()
    }
  }

  /** 关闭连接池，供进程优雅退出时释放 MySQL 连接。 */
  async close() {
    await this.pool.end()
  }
}
