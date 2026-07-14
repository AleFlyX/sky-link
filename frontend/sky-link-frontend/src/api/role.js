import { request } from '../utils/request'

export function getRoles(params) {
  return request.get('/api/v1/roles', params)
}

export function createRole(data) {
  return request.post('/api/v1/roles', data)
}

export function updateRole(roleId, data) {
  return request.put(`/api/v1/roles/${roleId}`, data)
}

export function deleteRole(roleId) {
  return request.delete(`/api/v1/roles/${roleId}`)
}

export function updateRolePermissions(roleId, permissionCodes) {
  return request.put(`/api/v1/roles/${roleId}/permissions`, { permissionCodes })
}
