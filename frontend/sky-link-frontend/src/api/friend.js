import { request } from '../utils/request'

export function getFriends(params) {
  return request.get('/friends', params)
}

export function addFriend(data) {
  return request.post('/friends/requests', data)
}

export function handleFriendRequest(requestId, data) {
  return request.put(`/friends/requests/${requestId}`, data)
}

export function deleteFriend(friendId) {
  return request.delete(`/friends/${friendId}`)
}

export function getFriendRequests(params) {
  return request.get('/friends/requests', params)
}

export async function getSentFriendRequests(params) {
  try {
    return await request.get('/friends/requests/sent', params)
  } catch (error) {
    const message = String(error?.message || '')
    if (!message.includes('400') && !message.includes('404')) {
      throw error
    }
    return request.get('/friends/requests/outgoing', params)
  }
}
