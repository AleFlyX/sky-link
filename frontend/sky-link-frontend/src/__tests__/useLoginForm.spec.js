import { describe, expect, it } from 'vitest'

import {
  sanitizeAuthAccount,
  sanitizeLoginPassword,
  useLoginForm,
} from '../views/auth/composables/useLoginForm'

describe('useLoginForm', () => {
  it('normalizes and trims the login account', () => {
    expect(sanitizeAuthAccount('  Ａlice@example.com\u200b\n')).toBe('Alice@example.com')
  })

  it('removes invisible characters from passwords without trimming visible spaces', () => {
    expect(sanitizeLoginPassword('  pass\u200bword  ')).toBe('  password  ')
  })

  it('returns sanitized credentials for submission', () => {
    const { form, getCredentials } = useLoginForm()

    form.account = '  demo_user\u200b '
    form.password = 'secret\u0000pass'

    expect(getCredentials()).toEqual({
      account: 'demo_user',
      password: 'secretpass',
    })
  })
})
