import { request } from '../utils/request'

export function getDocuments(params) {
  return request.get('/api/v1/documents', params)
}

export function getDocument(id) {
  return request.get(`/api/v1/documents/${id}`)
}

export function saveDocument(data, id) {
  return id ? updateDocument(id, data) : createDocument(data)
}

export function createDocument(data) {
  return request.post('/api/v1/documents', data)
}

export function updateDocument(documentId, data) {
  return request.put(`/api/v1/documents/${documentId}`, data)
}

export function deleteDocument(documentId) {
  return request.delete(`/api/v1/documents/${documentId}`)
}

export function setDocumentPermission(documentId, data) {
  return request.post(`/api/v1/documents/${documentId}/permissions`, data)
}

export function getDocumentPermissions(documentId) {
  return request.get(`/api/v1/documents/${documentId}/permissions`)
}

export function removeDocumentPermission(documentId, permissionId) {
  return request.delete(`/api/v1/documents/${documentId}/permissions/${permissionId}`)
}
