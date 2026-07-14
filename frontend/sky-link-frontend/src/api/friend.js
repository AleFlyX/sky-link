import { request } from '../utils/request'

export function getFriends(params) {
  return request.get('/api/v1/friends', params)
}

export function addFriend(data) {
  return request.post('/api/v1/friends/requests', data)
}

export function handleFriendRequest(requestId, data) {
  return request.put(`/api/v1/friends/requests/${requestId}`, data)
}

export function deleteFriend(friendId) {
  return request.delete(`/api/v1/friends/${friendId}`)
}

export function getFriendRequests(params) {
  return request.get('/api/v1/friends/requests', params)
}
