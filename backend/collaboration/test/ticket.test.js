import { createHmac } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import { verifyTicket } from '../src/ticket.js'

const secret = '01234567890123456789012345678901'
function ticket(documentId, permission = 'edit') {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url')
  const payload = Buffer.from(JSON.stringify({ iss: 'sky-link', aud: 'sky-link-collaboration', sub: '7', documentId,
    permission, displayName: 'Alice', jti: 'one', iat: 1, exp: Math.floor(Date.now() / 1000) + 60 })).toString('base64url')
  const unsigned = `${header}.${payload}`
  return `${unsigned}.${createHmac('sha256', secret).update(unsigned).digest('base64url')}`
}
describe('collaboration ticket boundary', () => {
  it('accepts only the document named by the ticket', () => {
    expect(verifyTicket(ticket(42), secret, '42')).toMatchObject({ userId: 7, documentId: 42, permission: 'edit' })
    expect(() => verifyTicket(ticket(42), secret, '43')).toThrow(/document mismatch/)
  })
  it('rejects read tickets with forged edit permission', () => {
    const parts = ticket(42, 'read').split('.')
    const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'))
    payload.permission = 'edit'
    const forged = `${parts[0]}.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.${parts[2]}`
    expect(() => verifyTicket(forged, secret, '42')).toThrow()
  })
  it('rejects signed tickets missing required identity claims', () => {
    const parts = ticket(42).split('.')
    const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8')); delete payload.jti
    const body = Buffer.from(JSON.stringify(payload)).toString('base64url'); const unsigned = `${parts[0]}.${body}`
    const signed = `${unsigned}.${createHmac('sha256', secret).update(unsigned).digest('base64url')}`
    expect(() => verifyTicket(signed, secret, '42')).toThrow(/required claims/)
  })
})
