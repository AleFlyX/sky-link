import mysql from 'mysql2/promise'
import * as Y from 'yjs'
import { markdownToYDoc, yDocToMarkdown } from './markdown.js'

/**
 * 协作仓库
 * @class
 * @param {mysql.Pool} pool - MySQL连接池
 */
export class CollaborationRepository {
  // 构造函数，接收一个MySQL连接池作为参数
  constructor(pool) { this.pool = pool }
  // 静态方法，用于创建一个新的CollaborationRepository实例
  static create(config) { return new CollaborationRepository(mysql.createPool(config)) }

  /**
   * 加载协作文档的状态，如果没有找到状态，则从文档内容生成状态并存储
   * @param {String|Number} documentId - 文档id
   * @returns 
   */
  async load(documentId) {
    // 从数据库中查询协作文档的状态 states是一个包含查询结果的数组
    // states数组包含的属性有 ydoc_state, state_vector, revision, updated_by等数据库表的所有字段
    const [states] = await this.pool.execute('SELECT ydoc_state FROM document_collaboration_state WHERE document_id = ?', [documentId])
    // states数组的长度大于0，说明找到了协作文档的状态，直接返回第一个状态的ydoc_state字段
    if (states.length) return new Uint8Array(states[0].ydoc_state)
    // 如果没有找到协作文档的状态，则从document表中查询文档内容
    const [documents] = await this.pool.execute('SELECT content FROM document WHERE document_id = ? AND is_deleted = 0', [documentId])
    // 如果没有找到文档内容，则抛出错误
    if (!documents.length) throw new Error('document not found')
    // 将文档内容转换为Yjs文档，并编码为状态更新
    const ydoc = markdownToYDoc(documents[0].content || '')
    // 将Yjs文档编码为状态更新，并存储到数据库中
    const state = Y.encodeStateAsUpdate(ydoc)
    // 调用store方法将状态存储到数据库中，updatedBy为null（updatedBy为null是因为）
    await this.store(documentId, state, null)
    // 返回状态更新
    return state
  }

  /**
   * 存储协作文档的状态
   * @param {} documentId 
   * @param {String} state 
   * @param {String} updatedBy 
   */
  async store(documentId, state, updatedBy) {
    // 如果状态的字节长度超过10 MiB，则抛出错误
    if (state.byteLength > 10 * 1024 * 1024) {
      const error = new Error('document exceeds the 10 MiB collaboration limit'); error.retryable = false; throw error
    }
    // 创建一个新的Yjs文档，并应用状态更新
    const ydoc = new Y.Doc(); Y.applyUpdate(ydoc, new Uint8Array(state))
    // 编码状态向量和将Yjs文档转换为Markdown格式
    const vector = Y.encodeStateVector(ydoc); const markdown = yDocToMarkdown(ydoc)
    const connection = await this.pool.getConnection()
    try {
      await connection.beginTransaction()
      await connection.execute(`INSERT INTO document_collaboration_state
        (document_id, ydoc_state, state_vector, revision, updated_by) VALUES (?, ?, ?, 1, ?)
        ON DUPLICATE KEY UPDATE ydoc_state=VALUES(ydoc_state), state_vector=VALUES(state_vector),
        revision=revision+1, updated_by=VALUES(updated_by)`, [documentId, Buffer.from(state), Buffer.from(vector), updatedBy])
      await connection.execute('UPDATE document SET content = ?, update_time = CURRENT_TIMESTAMP WHERE document_id = ? AND is_deleted = 0', [markdown, documentId])
      await connection.commit()
    } catch (error) { await connection.rollback(); throw error }
    finally { connection.release() }
  }

  async close() { await this.pool.end() }
}
