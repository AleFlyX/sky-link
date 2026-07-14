import { request } from '../utils/request'

export function getSessions() {
  return request.get('/messages/sessions')
}

export function getMessages(params) {
  return request.get('/messages', params)
}

export function sendMessage(data) {
  return request.post('/messages', data)
}

export function recallMessage(messageId) {
  return request.delete(`/messages/${messageId}`)
}

export function markMessagesRead(data) {
  return request.put('/messages/read', data)
}

export function getUnreadMessageCount() {
  return request.get('/messages/unread/count')
}
