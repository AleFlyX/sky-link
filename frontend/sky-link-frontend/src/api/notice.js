import { request } from '../utils/request'

export function getNotices(params) {
  return request.get('/api/v1/notices', params)
}

export function createNotice(data) {
  return request.post('/api/v1/notices', data)
}

export function markNoticeRead(id) {
  return request.post(`/api/v1/notices/${id}/read`)
}

export function getNotice(noticeId) {
  return request.get(`/api/v1/notices/${noticeId}`)
}

export function deleteNotice(noticeId) {
  return request.delete(`/api/v1/notices/${noticeId}`)
}

export function getUnreadNoticeCount() {
  return request.get('/api/v1/notices/unread/count')
}
