import { request } from '../utils/request'

export function getLoginLogs(params) {
  return request.get('/logs/login', params)
}

export function getOperationLogs(params) {
  return request.get('/logs/operation', params)
}
