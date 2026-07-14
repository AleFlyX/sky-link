import { request } from '../utils/request'

export function getRoles(params) {
  return request.get('/roles', params)
}

export function createRole(data) {
  return request.post('/roles', data)
}

export function updateRole(roleId, data) {
  return request.put(`/roles/${roleId}`, data)
}

export function deleteRole(roleId) {
  return request.delete(`/roles/${roleId}`)
}

export function updateRolePermissions(roleId, permissionCodes) {
  return request.put(`/roles/${roleId}/permissions`, { permissionCodes })
}
