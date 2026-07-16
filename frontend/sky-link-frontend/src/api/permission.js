import { request } from '../utils/request'

export function getPermissions(params) {
  return request.get('/permissions', params)
}

export function getPermissionPage(params) {
  return request.get('/permissions/page', params)
}

export function getPermission(permissionId) {
  return request.get(`/permissions/${permissionId}`)
}

export function createPermission(data) {
  return request.post('/permissions', data)
}

export function updatePermission(permissionId, data) {
  return request.put(`/permissions/${permissionId}`, data)
}

export function deletePermission(permissionId) {
  return request.delete(`/permissions/${permissionId}`)
}
