import { request } from '../utils/request'

export function getCurrentUser() {
  return request.get('/users/me')
}

export function updateCurrentUser(data) {
  return request.put('/users/me', data)
}

export function changePassword(data) {
  return request.put('/users/me/password', data)
}

export function getUsers(params) {
  return request.get('/users', params)
}

export function createUser(data) {
  return request.post('/users', data)
}

export function getUser(userId) {
  return request.get(`/users/${userId}`)
}

export function updateUserStatus(userId, status) {
  return request.put(`/users/${userId}/status`, { status })
}

export function deleteUser(userId) {
  return request.delete(`/users/${userId}`)
}

export function assignUserRoles(userId, roleIds) {
  return request.post(`/users/${userId}/roles`, { roleIds })
}

export function removeUserRole(userId, roleId) {
  return request.delete(`/users/${userId}/roles/${roleId}`)
}
