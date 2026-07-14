import { request } from '../utils/request'

export function getTasks(params) {
  return request.get('/tasks', params)
}

export function createTask(data) {
  return request.post('/tasks', data)
}

export function getTask(taskId) {
  return request.get(`/tasks/${taskId}`)
}

export function updateTask(taskId, data) {
  return request.put(`/tasks/${taskId}`, data)
}

export function updateTaskStatus(taskId, status) {
  return request.put(`/tasks/${taskId}/status`, { status })
}

export function deleteTask(taskId) {
  return request.delete(`/tasks/${taskId}`)
}

export function getTaskStatistics(params) {
  return request.get('/tasks/statistics', params)
}
