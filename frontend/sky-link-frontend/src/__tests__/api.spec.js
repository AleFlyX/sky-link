import { beforeEach, describe, expect, it, vi } from 'vitest'

import { login, refreshToken } from '../api/auth'
import { downloadFile } from '../api/file'
import { sendMessage } from '../api/message'
import { updateRolePermissions } from '../api/role'
import { assignUserRoles } from '../api/user'
import { cookieRequest, request } from '../utils/request'

vi.mock('../utils/request', () => ({
  cookieRequest: {
    post: vi.fn(),
  },
  request: {
    delete: vi.fn(),
    download: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('business API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('assigns multiple roles with the documented payload', () => {
    assignUserRoles(10, [1, 2])

    expect(request.post).toHaveBeenCalledWith('/users/10/roles', { roleIds: [1, 2] })
  })

  it('updates role permissions by permission code', () => {
    updateRolePermissions(3, ['user:read', 'user:write'])

    expect(request.put).toHaveBeenCalledWith('/roles/3/permissions', {
      permissionCodes: ['user:read', 'user:write'],
    })
  })

  it('passes the documented message payload through unchanged', () => {
    const data = { receiverId: 12, messageType: 'text', content: 'hello' }

    sendMessage(data)

    expect(request.post).toHaveBeenCalledWith('/messages', data)
  })

  it('uses the raw download helper for file content', () => {
    downloadFile(8)

    expect(request.download).toHaveBeenCalledWith('/files/8/download')
  })

  it('posts login credentials to the auth endpoint', () => {
    login('demo', 'secret')

    expect(request.post).toHaveBeenCalledWith('/auth/login', {
      account: 'demo',
      password: 'secret',
    })
  })

  it('refreshes access token through the http-only refresh cookie', () => {
    refreshToken()

    expect(cookieRequest.post).toHaveBeenCalledWith('/auth/refresh')
    expect(request.post).not.toHaveBeenCalledWith('/auth/refresh')
  })
})
