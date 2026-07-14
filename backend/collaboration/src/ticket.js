import { createHmac, timingSafeEqual } from 'node:crypto'

const decode = (value) => JSON.parse(Buffer.from(value, 'base64url').toString('utf8'))
const sign = (value, secret) => createHmac('sha256', secret).update(value).digest('base64url')

export function verifyTicket(token, secret, expectedDocumentId, now = Date.now()) {
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
