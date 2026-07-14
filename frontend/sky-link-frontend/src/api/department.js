import { request } from '../utils/request'

export function getDepartments(params) {
  return request.get('/api/v1/departments', params)
}

export function createDepartment(data) {
  return request.post('/api/v1/departments', data)
}

export function updateDepartment(departmentId, data) {
  return request.put(`/api/v1/departments/${departmentId}`, data)
}

export function deleteDepartment(departmentId) {
  return request.delete(`/api/v1/departments/${departmentId}`)
}

export function getDepartmentMembers(departmentId, params) {
  return request.get(`/api/v1/departments/${departmentId}/members`, params)
}
