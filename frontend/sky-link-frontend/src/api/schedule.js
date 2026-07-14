import { request } from '../utils/request'

export function getSchedules(params) {
  return request.get('/schedules', params)
}

export function createSchedule(data) {
  return request.post('/schedules', data)
}

export function getSchedule(scheduleId) {
  return request.get(`/schedules/${scheduleId}`)
}

export function updateSchedule(scheduleId, data) {
  return request.put(`/schedules/${scheduleId}`, data)
}

export function deleteSchedule(scheduleId) {
  return request.delete(`/schedules/${scheduleId}`)
}
