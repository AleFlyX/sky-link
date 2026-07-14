import { request } from '../utils/request'

export function getSchedules(params) {
  return request.get('/api/v1/schedules', params)
}

export function createSchedule(data) {
  return request.post('/api/v1/schedules', data)
}

export function getSchedule(scheduleId) {
  return request.get(`/api/v1/schedules/${scheduleId}`)
}

export function updateSchedule(scheduleId, data) {
  return request.put(`/api/v1/schedules/${scheduleId}`, data)
}

export function deleteSchedule(scheduleId) {
  return request.delete(`/api/v1/schedules/${scheduleId}`)
}
