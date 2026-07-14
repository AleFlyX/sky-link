import { beforeEach, describe, expect, it, vi } from 'vitest'

import { downloadFile } from '../api/file'
import { sendMessage } from '../api/message'
import { updateRolePermissions } from '../api/role'
import { assignUserRoles } from '../api/user'
import { request } from '../utils/request'

vi.mock('../utils/request', () => ({
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

    expect(request.post).toHaveBeenCalledWith('/api/v1/users/10/roles', { roleIds: [1, 2] })
  })

  it('updates role permissions by permission code', () => {
    updateRolePermissions(3, ['user:read', 'user:write'])

    expect(request.put).toHaveBeenCalledWith('/api/v1/roles/3/permissions', {
      permissionCodes: ['user:read', 'user:write'],
    })
  })

  it('passes the documented message payload through unchanged', () => {
    const data = { receiverId: 12, messageType: 'text', content: 'hello' }

    sendMessage(data)

    expect(request.post).toHaveBeenCalledWith('/api/v1/messages', data)
  })

  it('uses the raw download helper for file content', () => {
    downloadFile(8)

    expect(request.download).toHaveBeenCalledWith('/api/v1/files/8/download')
  })
})
