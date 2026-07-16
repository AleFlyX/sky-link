import { createHmac, timingSafeEqual } from 'node:crypto'

const decode = (value) => JSON.parse(Buffer.from(value, 'base64url').toString('utf8'))
const sign = (value, secret) => createHmac('sha256', secret).update(value).digest('base64url')

/**
 * 验证协作票据
 * @param {String} token 
 * @param {} secret 
 * @param {*} expectedDocumentId 
 * @param {*} now 
 * @returns 
 */
export function verifyTicket(token, secret, expectedDocumentId, now = Date.now()) {
  // 将票据拆分为三部分，并进行基本的验证，包括检查长度、签名、头部和负载的有效性，以及时间戳和权限的验证
  // token是一个JWT格式的字符串，包含三部分：头部、负载和签名。secret是用于验证签名的密钥。expectedDocumentId是预期的文档ID，用于验证票据是否与特定文档匹配。now是当前时间戳，用于验证票据的有效期。函数返回一个对象，包含用户ID、文档ID、权限、显示名称、票据ID和最后授权时间等信息。
  const parts = String(token || '').split('.')
  if (parts.length !== 3) throw new Error('invalid collaboration ticket')
  const actual = Buffer.from(parts[2])
  const expected = Buffer.from(sign(`${parts[0]}.${parts[1]}`, secret))
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) throw new Error('invalid collaboration ticket')
  const header = decode(parts[0])
  if (header.alg !== 'HS256' || header.typ !== 'JWT') throw new Error('invalid collaboration ticket algorithm')
  const payload = decode(parts[1])
  if (payload.iss !== 'sky-link' || payload.aud !== 'sky-link-collaboration') throw new Error('invalid collaboration ticket audience')
  const userId = Number(payload.sub); const documentId = Number(payload.documentId)
  const issuedAt = Number(payload.iat) * 1000; const expiresAt = Number(payload.exp) * 1000
  if (!Number.isInteger(userId) || userId < 1 || !Number.isInteger(documentId) || documentId < 1 || !payload.jti || !payload.displayName)
    throw new Error('collaboration ticket is missing required claims')
  if (!Number.isFinite(issuedAt) || issuedAt > now + 30000 || !Number.isFinite(expiresAt) || expiresAt <= now || expiresAt <= issuedAt)
    throw new Error('collaboration ticket timestamps are invalid')
  if (String(documentId) !== String(expectedDocumentId)) throw new Error('collaboration ticket document mismatch')
  if (!['read', 'edit', 'manage'].includes(payload.permission)) throw new Error('invalid collaboration permission')
  return { userId, documentId, permission: payload.permission,
    displayName: payload.displayName, ticketId: payload.jti, lastAuthorizedAt: now }
}
