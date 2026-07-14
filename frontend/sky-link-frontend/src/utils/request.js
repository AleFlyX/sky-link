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

    if (
      payload &&
      typeof payload === 'object' &&
      'code' in payload &&
      ![0, 200].includes(payload.code)
    ) {
      return Promise.reject(new Error(payload.message || 'Request failed'))
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

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export { TOKEN_KEY, service }
