import { request } from '../utils/request'

export function getCurrentUser() {
  return request.get('/api/v1/users/me')
}

export function updateCurrentUser(data) {
  return request.put('/api/v1/users/me', data)
}

export function changePassword(data) {
  return request.put('/api/v1/users/me/password', data)
}

export function getUsers(params) {
  return request.get('/api/v1/users', params)
}

export function createUser(data) {
  return request.post('/api/v1/users', data)
}

export function getUser(userId) {
  return request.get(`/api/v1/users/${userId}`)
}

export function updateUserStatus(userId, status) {
  return request.put(`/api/v1/users/${userId}/status`, { status })
}

export function deleteUser(userId) {
  return request.delete(`/api/v1/users/${userId}`)
}

export function assignUserRoles(userId, roleIds) {
  return request.post(`/api/v1/users/${userId}/roles`, { roleIds })
}

export function removeUserRole(userId, roleId) {
  return request.delete(`/api/v1/users/${userId}/roles/${roleId}`)
}
