import { request } from '../utils/request'

export function uploadFile(data) {
  return request.post('/files', data)
}

export function getFiles(params) {
  return request.get('/files', params)
}

export function createFile(data) {
  return uploadFile(data)
}

export function getFile(fileId) {
  return request.get(`/files/${fileId}`)
}

export function downloadFile(fileId) {
  return request.download(`/files/${fileId}/download`)
}

export function deleteFile(fileId) {
  return request.delete(`/files/${fileId}`)
}

export function shareFile(fileId, data) {
  return request.post(`/files/${fileId}/share`, data)
}

export function getFileShares(params) {
  return request.get('/files/shares', params)
}

export function deleteFileShare(shareId) {
  return request.delete(`/files/shares/${shareId}`)
}
