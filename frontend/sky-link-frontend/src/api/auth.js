import { request } from '../utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(account, password) {
  return request.post('/auth/login', { account, password })
}

export function refreshToken(refreshTokenValue) {
  return request.post('/auth/refresh', { refreshToken: refreshTokenValue })
}

export function logout() {
  return request.post('/auth/logout')
}
