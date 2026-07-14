import { cookieRequest, request } from '../utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(account, password) {
  return request.post('/auth/login', { account, password })
}

export function refreshToken() {
  return cookieRequest.post('/auth/refresh')
}

export function logout() {
  return request.post('/auth/logout')
}
