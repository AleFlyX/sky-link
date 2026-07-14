import { request } from '../utils/request'

export function getSessions() {
  return request.get('/api/v1/messages/sessions')
}

export function getMessages(params) {
  return request.get('/api/v1/messages', params)
}

export function sendMessage(data) {
  return request.post('/api/v1/messages', data)
}

export function recallMessage(messageId) {
  return request.delete(`/api/v1/messages/${messageId}`)
}

export function markMessagesRead(data) {
  return request.put('/api/v1/messages/read', data)
}

export function getUnreadMessageCount() {
  return request.get('/api/v1/messages/unread/count')
}
