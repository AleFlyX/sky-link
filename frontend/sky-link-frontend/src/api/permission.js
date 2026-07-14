import { request } from '../utils/request'

export function getPermissions() {
  return request.get('/permissions')
}
