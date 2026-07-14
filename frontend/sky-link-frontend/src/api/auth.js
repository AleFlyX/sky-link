import { request } from '../utils/request'

export function register(data) {
  return request.post('/api/v1/auth/register', data)
}

export function login(account, password) {
  return request.post('/api/v1/auth/login', { account, password })
}

export function refreshToken(refreshTokenValue) {
  return request.post('/api/v1/auth/refresh', { refreshToken: refreshTokenValue })
}

export function logout() {
  return request.post('/api/v1/auth/logout')
}
