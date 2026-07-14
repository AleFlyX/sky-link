import mysql from 'mysql2/promise'
import * as Y from 'yjs'
import { markdownToYDoc, yDocToMarkdown } from './markdown.js'

export class CollaborationRepository {
  constructor(pool) { this.pool = pool }
  static create(config) { return new CollaborationRepository(mysql.createPool(config)) }

  async load(documentId) {
    const [states] = await this.pool.execute('SELECT ydoc_state FROM document_collaboration_state WHERE document_id = ?', [documentId])
    if (states.length) return new Uint8Array(states[0].ydoc_state)
    const [documents] = await this.pool.execute('SELECT content FROM document WHERE document_id = ? AND is_deleted = 0', [documentId])
    if (!documents.length) throw new Error('document not found')
    const ydoc = markdownToYDoc(documents[0].content || '')
    const state = Y.encodeStateAsUpdate(ydoc)
    await this.store(documentId, state, null)
    return state
  }

  async store(documentId, state, updatedBy) {
    if (state.byteLength > 10 * 1024 * 1024) {
      const error = new Error('document exceeds the 10 MiB collaboration limit'); error.retryable = false; throw error
    }
    const ydoc = new Y.Doc(); Y.applyUpdate(ydoc, new Uint8Array(state))
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
