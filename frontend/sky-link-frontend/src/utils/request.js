import axios from 'axios'

const TOKEN_KEY = 'skylink_token'
const UNAUTHORIZED_PATH = `${import.meta.env.BASE_URL}401`

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

const cookieService = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  withCredentials: true,
})

function attachAuthToken(config) {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
}

function handleRequestError(error) {
  return Promise.reject(error)
}

function handleResponse(response) {
  const payload = response.data

  if (response.config.rawResponse) {
    if (response.status !== 200) {
      return Promise.reject(new Error(`Unexpected response status: ${response.status}`))
    }
    return payload
  }

  if (!payload || typeof payload !== 'object' || payload.code !== 200) {
    return Promise.reject(new Error(payload?.message || 'Invalid response'))
  }

  return payload
}

function handleResponseError(error) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message || error.message || 'Request failed'

    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)

      if (window.location.pathname !== UNAUTHORIZED_PATH) {
        window.location.replace(UNAUTHORIZED_PATH)
      }
    }

    return Promise.reject(new Error(message))
  }

  return Promise.reject(error)
}

service.interceptors.request.use(attachAuthToken, handleRequestError)

service.interceptors.response.use(handleResponse, handleResponseError)
cookieService.interceptors.response.use(handleResponse, handleResponseError)

export function request(urlOrConfig, options = {}) {
  if (typeof urlOrConfig === 'string') {
    const { body, ...restOptions } = options

    return service({
      url: urlOrConfig,
      method: restOptions.method || 'get',
      data: body,
      ...restOptions,
    })
  }

  return service(urlOrConfig)
}

request.get = (url, params, config = {}) => service.get(url, { ...config, params })
request.post = (url, data, config = {}) => service.post(url, data, config)
request.put = (url, data, config = {}) => service.put(url, data, config)
request.delete = (url, config = {}) => service.delete(url, config)
request.download = (url, params, config = {}) =>
  service.get(url, { ...config, params, responseType: 'blob', rawResponse: true })

export function cookieRequest(urlOrConfig, options = {}) {
  if (typeof urlOrConfig === 'string') {
    const { body, ...restOptions } = options

    return cookieService({
      url: urlOrConfig,
      method: restOptions.method || 'get',
      data: body,
      ...restOptions,
    })
  }

  return cookieService(urlOrConfig)
}

cookieRequest.post = (url, data, config = {}) => cookieService.post(url, data, config)

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export { TOKEN_KEY, cookieService, service }
