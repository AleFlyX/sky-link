import { request } from '../utils/request'

export function getDocuments(params) {
  return request.get('/documents', params)
}

export function getDocument(id) {
  return request.get(`/documents/${id}`)
}

export function saveDocument(data, id) {
  return id ? updateDocument(id, data) : createDocument(data)
}

export function createDocument(data) {
  return request.post('/documents', data)
}

export function updateDocument(documentId, data) {
  return request.put(`/documents/${documentId}`, data)
}

export function deleteDocument(documentId) {
  return request.delete(`/documents/${documentId}`)
}

export function setDocumentPermission(documentId, data) {
  return request.post(`/documents/${documentId}/permissions`, data)
}

export function getDocumentPermissions(documentId) {
  return request.get(`/documents/${documentId}/permissions`)
}

export function removeDocumentPermission(documentId, permissionId) {
  return request.delete(`/documents/${documentId}/permissions/${permissionId}`)
}
