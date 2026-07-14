import { request } from '../utils/request'

export function getOverview() {
  return request.get('/statistics/overview')
}
