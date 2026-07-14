import axios from 'axios'

const TOKEN_KEY = 'skylink_token'
const UNAUTHORIZED_PATH = `${import.meta.env.BASE_URL}401`

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response) => {
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
  },
  (error) => {
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
  },
)

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

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export { TOKEN_KEY, service }
