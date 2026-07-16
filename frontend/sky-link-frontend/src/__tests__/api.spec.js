import { beforeEach, describe, expect, it, vi } from 'vitest'

import { login, refreshToken } from '../api/auth'
import { downloadFile } from '../api/file'
import { getSentFriendRequests, handleFriendRequest } from '../api/friend'
import { sendMessage } from '../api/message'
import { createPermission, deletePermission, getPermissionPage, updatePermission } from '../api/permission'
import { updateRolePermissions } from '../api/role'
import { assignUserRoles } from '../api/user'
import { createCollaborationTicket, setDocumentPermission, setDocumentGroupPermission } from '../api/document'
import {
  addGroupMembers,
  deleteGroup,
  getGroup,
  getGroupMembers,
  leaveGroup,
  removeGroupMember,
  updateGroup,
  updateGroupMemberRole,
} from '../api/group'
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

  it('uses the friend request inbox and sent-list contracts', () => {
    handleFriendRequest(5, { action: 'accept' })
    getSentFriendRequests({ page: 1, size: 10 })

    expect(request.put).toHaveBeenCalledWith('/friends/requests/5', { action: 'accept' })
    expect(request.get).toHaveBeenCalledWith('/friends/requests/sent', { page: 1, size: 10 })
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

  it('uses document-scoped permission and collaboration contracts', () => {
    setDocumentPermission(9, 12, 'edit')
    setDocumentGroupPermission(9, 3, 'read')
    createCollaborationTicket(9)

    expect(request.put).toHaveBeenCalledWith('/documents/9/permissions/12', { permissionType: 'edit' })
    expect(request.put).toHaveBeenCalledWith('/documents/9/group-permissions/3', { permissionType: 'read' })
    expect(request.post).toHaveBeenCalledWith('/documents/9/collaboration-ticket')
  })

  it('uses the documented group management contracts', () => {
    getGroup(7)
    updateGroup(7, { groupName: '新群名', notice: '新公告' })
    getGroupMembers(7, { page: 1, size: 6 })
    addGroupMembers(7, { userIds: [1002, 1003] })
    updateGroupMemberRole(7, 1002, 'admin')
    removeGroupMember(7, 1003)
    leaveGroup(7)
    deleteGroup(7)

    expect(request.get).toHaveBeenCalledWith('/groups/7')
    expect(request.put).toHaveBeenCalledWith('/groups/7', {
      groupName: '新群名',
      notice: '新公告',
    })
    expect(request.get).toHaveBeenCalledWith('/groups/7/members', { page: 1, size: 6 })
    expect(request.post).toHaveBeenCalledWith('/groups/7/members', { userIds: [1002, 1003] })
    expect(request.put).toHaveBeenCalledWith('/groups/7/members/1002/role', { role: 'admin' })
    expect(request.delete).toHaveBeenCalledWith('/groups/7/members/1003')
    expect(request.delete).toHaveBeenCalledWith('/groups/7/members/me')
    expect(request.delete).toHaveBeenCalledWith('/groups/7')
  })

  it('uses the documented permission management contracts', () => {
    getPermissionPage({ page: 2, size: 20, permissionCode: 'role:list' })
    createPermission({
      permissionName: '角色列表',
      permissionCode: 'role:list',
      permissionType: 3,
      sortNo: 10,
    })
    updatePermission(6, {
      permissionName: '角色查看',
      permissionCode: 'role:list',
      permissionType: 3,
      sortNo: 20,
    })
    deletePermission(6)

    expect(request.get).toHaveBeenCalledWith('/permissions/page', {
      page: 2,
      size: 20,
      permissionCode: 'role:list',
    })
    expect(request.post).toHaveBeenCalledWith('/permissions', {
      permissionName: '角色列表',
      permissionCode: 'role:list',
      permissionType: 3,
      sortNo: 10,
    })
    expect(request.put).toHaveBeenCalledWith('/permissions/6', {
      permissionName: '角色查看',
      permissionCode: 'role:list',
      permissionType: 3,
      sortNo: 20,
    })
    expect(request.delete).toHaveBeenCalledWith('/permissions/6')
  })
})
