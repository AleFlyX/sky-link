import { request } from '../utils/request'

export function getGroups(params) {
  return request.get('/api/v1/groups', params)
}

export function createGroup(data) {
  return request.post('/api/v1/groups', data)
}

export function getGroup(groupId) {
  return request.get(`/api/v1/groups/${groupId}`)
}

export function updateGroup(groupId, data) {
  return request.put(`/api/v1/groups/${groupId}`, data)
}

export function deleteGroup(groupId) {
  return request.delete(`/api/v1/groups/${groupId}`)
}

export function getGroupMembers(groupId, params) {
  return request.get(`/api/v1/groups/${groupId}/members`, params)
}

export function addGroupMembers(groupId, data) {
  return request.post(`/api/v1/groups/${groupId}/members`, data)
}

export function removeGroupMember(groupId, userId) {
  return request.delete(`/api/v1/groups/${groupId}/members/${userId}`)
}

export function updateGroupMemberRole(groupId, userId, role) {
  return request.put(`/api/v1/groups/${groupId}/members/${userId}/role`, { role })
}

export function leaveGroup(groupId) {
  return request.delete(`/api/v1/groups/${groupId}/members/me`)
}
