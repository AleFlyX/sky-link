import { request } from '../utils/request'

export function getLoginLogs(params) {
  return request.get('/api/v1/logs/login', params)
}

export function getOperationLogs(params) {
  return request.get('/api/v1/logs/operation', params)
}
