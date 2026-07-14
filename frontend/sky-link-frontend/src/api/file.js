import { request } from '../utils/request'

export function uploadFile(data) {
  return request.post('/api/v1/files', data)
}

export function getFiles(params) {
  return request.get('/api/v1/files', params)
}

export function createFile(data) {
  return uploadFile(data)
}

export function getFile(fileId) {
  return request.get(`/api/v1/files/${fileId}`)
}

export function downloadFile(fileId) {
  return request.download(`/api/v1/files/${fileId}/download`)
}

export function deleteFile(fileId) {
  return request.delete(`/api/v1/files/${fileId}`)
}

export function shareFile(fileId, data) {
  return request.post(`/api/v1/files/${fileId}/share`, data)
}

export function getFileShares(params) {
  return request.get('/api/v1/files/shares', params)
}

export function deleteFileShare(shareId) {
  return request.delete(`/api/v1/files/shares/${shareId}`)
}
