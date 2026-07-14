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

export function setDocumentPermission(documentId, userId, permissionType) {
  return request.put(`/documents/${documentId}/permissions/${userId}`, { permissionType })
}

export function getDocumentPermissions(documentId) {
  return request.get(`/documents/${documentId}/permissions`)
}

export function removeDocumentPermission(documentId, userId) {
  return request.delete(`/documents/${documentId}/permissions/${userId}`)
}

export function setDocumentGroupPermission(documentId, groupId, permissionType) {
  return request.put(`/documents/${documentId}/group-permissions/${groupId}`, { permissionType })
}

export function removeDocumentGroupPermission(documentId, groupId) {
  return request.delete(`/documents/${documentId}/group-permissions/${groupId}`)
}

export function createCollaborationTicket(documentId) {
  return request.post(`/documents/${documentId}/collaboration-ticket`)
}
