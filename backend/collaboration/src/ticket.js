// Node 内置 crypto：HMAC 用于验签，timingSafeEqual 用于避免按字节提前返回的比较方式。
import { createHmac, timingSafeEqual } from 'node:crypto'

// 将 JWT 的 Base64URL 段还原为 UTF-8 JSON 对象。解码/JSON 失败会由调用方视为无效票据。
const decode = (value) => JSON.parse(Buffer.from(value, 'base64url').toString('utf8'))

// 对“header.payload”使用共享密钥计算 HS256 签名，输出与 JWT 第三段相同的 Base64URL 格式。
const sign = (value, secret) => createHmac('sha256', secret).update(value).digest('base64url')

/**
 * 校验后端为协同 WebSocket 签发的短期票据，并把可信字段整理成连接上下文。
 *
 * 这里不会相信前端直接传来的 userId、documentId 或 permission；它们都必须来自验签成功的 payload。
 *
 * @param {string} token WebSocket 客户端带来的 `header.payload.signature` 票据。
 * @param {string} secret 仅 BFF 与签发票据的后端知道的 HMAC 密钥。
 * @param {string|number} expectedDocumentId 当前 WebSocket 正在请求的文档 ID。
 * @param {number} now 当前毫秒时间戳；默认取 Date.now，测试可传入固定值。
 * @returns {{ userId: number, documentId: number, permission: string, displayName: string, ticketId: string, lastAuthorizedAt: number }}
 */
export function verifyTicket(token, secret, expectedDocumentId, now = Date.now()) {
  // JWT 恰好应有三段；空值也会被转成空字符串后拒绝。
  const parts = String(token || '').split('.')
  if (parts.length !== 3) throw new Error('invalid collaboration ticket')

  // 先计算前两段应有的签名，再与客户端传来的第三段比较。
  const actual = Buffer.from(parts[2])
  const expected = Buffer.from(sign(`${parts[0]}.${parts[1]}`, secret))
  // 长度不同不能调用 timingSafeEqual；长度相同则以恒定时间比较，降低时序侧信道风险。
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
    throw new Error('invalid collaboration ticket')
  }

  // 签名通过后才解码 header；本服务只接受固定的 HS256 JWT 格式。
  const header = decode(parts[0])
  if (header.alg !== 'HS256' || header.typ !== 'JWT') {
    throw new Error('invalid collaboration ticket algorithm')
  }

  // payload 现在可被信任为“由持有 secret 的后端签发”，但仍要检查业务字段是否完整、是否过期。
  const payload = decode(parts[1])
  if (payload.iss !== 'sky-link' || payload.aud !== 'sky-link-collaboration') {
    throw new Error('invalid collaboration ticket audience')
  }

  // 将 JWT 中可能是字符串的 sub/documentId 转成数字，后续数据库和授权接口使用数字 ID。
  const userId = Number(payload.sub)
  const documentId = Number(payload.documentId)
  // JWT 标准 iat/exp 单位是秒；JavaScript Date.now 单位是毫秒，因此乘 1000。
  const issuedAt = Number(payload.iat) * 1000
  const expiresAt = Number(payload.exp) * 1000

  // 身份、目标文档、票据唯一 ID、展示名任一缺失，都不建立协同连接。
  if (!Number.isInteger(userId) || userId < 1 || !Number.isInteger(documentId) || documentId < 1 || !payload.jti || !payload.displayName) {
    throw new Error('collaboration ticket is missing required claims')
  }
  // 允许最多 30 秒的服务器时钟差，但不接受未来签发、过期或“过期早于签发”的票据。
  if (!Number.isFinite(issuedAt) || issuedAt > now + 30000 || !Number.isFinite(expiresAt) || expiresAt <= now || expiresAt <= issuedAt) {
    throw new Error('collaboration ticket timestamps are invalid')
  }
  // 票据只能用于它明确绑定的文档，不能拿文档 A 的票据打开文档 B。
  if (String(documentId) !== String(expectedDocumentId)) {
    throw new Error('collaboration ticket document mismatch')
  }
  // 仅允许三档协作权限；未知字符串即使签名有效也不应拥有能力。
  if (!['read', 'edit', 'manage'].includes(payload.permission)) {
    throw new Error('invalid collaboration permission')
  }

  // 返回后续 Hocuspocus 钩子真正需要的最小上下文；lastAuthorizedAt 用于授权服务短暂故障时的兜底计时。
  return {
    userId,
    documentId,
    permission: payload.permission,
    displayName: payload.displayName,
    ticketId: payload.jti,
    lastAuthorizedAt: now,
  }
}
