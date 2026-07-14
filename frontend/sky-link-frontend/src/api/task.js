import { request } from '../utils/request'

export function getTasks(params) {
  return request.get('/api/v1/tasks', params)
}

export function createTask(data) {
  return request.post('/api/v1/tasks', data)
}

export function getTask(taskId) {
  return request.get(`/api/v1/tasks/${taskId}`)
}

export function updateTask(taskId, data) {
  return request.put(`/api/v1/tasks/${taskId}`, data)
}

export function updateTaskStatus(taskId, status) {
  return request.put(`/api/v1/tasks/${taskId}/status`, { status })
}

export function deleteTask(taskId) {
  return request.delete(`/api/v1/tasks/${taskId}`)
}

export function getTaskStatistics(params) {
  return request.get('/api/v1/tasks/statistics', params)
}
