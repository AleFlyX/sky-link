import { request } from '../utils/request'

export function getOverview() {
  return request('/api/v1/statistics/overview')
}
