import { request } from '../utils/request'

export function getDepartments(params) {
  return request.get('/departments', params)
}

export function createDepartment(data) {
  return request.post('/departments', data)
}

export function updateDepartment(departmentId, data) {
  return request.put(`/departments/${departmentId}`, data)
}

export function deleteDepartment(departmentId) {
  return request.delete(`/departments/${departmentId}`)
}

export function getDepartmentMembers(departmentId, params) {
  return request.get(`/departments/${departmentId}/members`, params)
}
