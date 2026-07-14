import { request } from '../utils/request'

export function getNotices(params) {
  return request.get('/notices', params)
}

export function createNotice(data) {
  return request.post('/notices', data)
}

export function markNoticeRead(id) {
  return request.post(`/notices/${id}/read`)
}

export function getNotice(noticeId) {
  return request.get(`/notices/${noticeId}`)
}

export function deleteNotice(noticeId) {
  return request.delete(`/notices/${noticeId}`)
}

export function getUnreadNoticeCount() {
  return request.get('/notices/unread/count')
}
