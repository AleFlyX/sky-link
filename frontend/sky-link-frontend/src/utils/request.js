import axios from 'axios'

// 本地存储里保存的 access token 键名。
const TOKEN_KEY = 'skylink_token'
// 401 未授权页的路由地址，通常用于 token 失效或权限不足时的兜底展示。
const UNAUTHORIZED_PATH = `${import.meta.env.BASE_URL}401`
// 识别认证接口的前缀，避免登录、注册、刷新这类接口被统一重定向打断。
const AUTH_PATH_PREFIX = '/auth/'

// 业务请求实例：默认携带 access token，走统一的响应包裹处理。
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

// Cookie 请求实例：专门用于依赖 httpOnly refresh cookie 的接口。
// 这个实例开启 withCredentials，确保浏览器会自动带上刷新 cookie。
const cookieService = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  withCredentials: true,
})

// 刷新 token 的并发锁。
// 当多个请求同时收到 401 时，只发起一次 refresh，请求都等待同一个 Promise。
let refreshAccessTokenPromise = null

// 请求拦截器：每次发起业务请求前，从 localStorage 里读取最新 token，
// 然后放到 Authorization 头中。
function attachAuthToken(config) {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    // 某些请求可能没有显式 headers，这里先兜底初始化，避免赋值时报错。
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
}

// 请求拦截器失败时的兜底处理：这里直接把错误交给后续调用方。
function handleRequestError(error) {
  return Promise.reject(error)
}

// 判断某个请求发生 401 后，是否需要跳转到未授权页。
// 认证接口本身不应该被跳转逻辑打断，否则登录/刷新流程会互相干扰。
function shouldRedirectToUnauthorized(responseConfig) {
  const requestUrl = String(responseConfig?.url || '')
  const normalizedUrl = requestUrl.split('?')[0]

  return !(normalizedUrl === '/auth' || normalizedUrl.startsWith(AUTH_PATH_PREFIX))
}

// 统一导出一个跳转器，方便在多个地方复用，也便于测试时 mock。
export const unauthorizedRedirect = {
  toUnauthorized() {
    // 避免重复跳转同一个页面，减少无意义的 history 替换。
    if (window.location.pathname !== UNAUTHORIZED_PATH) {
      window.location.replace(UNAUTHORIZED_PATH)
    }
  },
}

// 响应成功时的统一处理：
// 1. 对下载这类 rawResponse 请求，直接返回原始 data。
// 2. 对普通业务请求，约定后端返回 code===200 才算成功。
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

// 兼容后端不同的 token 返回字段命名：
// 有些接口返回 accessToken，有些历史结构可能叫 token。
function extractAccessToken(payload) {
  return (
    payload?.accessToken || payload?.token || payload?.data?.accessToken || payload?.data?.token
  )
}

// 执行 refresh 流程：
// 1. 请求 /auth/refresh，依赖 refresh cookie。
// 2. 从响应里提取新的 access token。
// 3. 写回 localStorage，供后续业务请求自动携带。
// 4. 用 finally 清理并发锁，允许下一次 refresh 重新发起。
function refreshAccessToken() {
  if (!refreshAccessTokenPromise) {
    refreshAccessTokenPromise = cookieService
      .post('/auth/refresh', undefined, {
        // 刷新接口本身不再进入“401 自动刷新”分支，防止无限递归。
        skipAuthRefresh: true,
      })
      .then((response) => {
        const nextToken = extractAccessToken(response)

        if (!nextToken) {
          throw new Error('Refresh response did not include an access token')
        }

        setToken(nextToken)
        return nextToken
      })
      .finally(() => {
        refreshAccessTokenPromise = null
      })
  }

  return refreshAccessTokenPromise
}

// 响应错误统一处理：
// - 普通错误：转换成可读的 Error。
// - 401：优先尝试 refresh 并重试原请求。
// - refresh 失败：清理 token 并跳转未授权页。
function handleResponseError(error) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message || error.message || 'Request failed'
    const responseConfig = error.response?.config || error.config

    if (error.response?.status === 401) {
      // 仅对“业务请求”做自动刷新；认证接口和显式禁用的请求直接走原逻辑。
      if (!responseConfig?.skipAuthRefresh && shouldRedirectToUnauthorized(responseConfig)) {
        return refreshAccessToken()
          .then(() => {
            // refresh 成功后，用原始请求配置重放一次请求。
            // 这里保留原参数、路径、method、body 等信息，只额外标记 skipAuthRefresh。
            const retryConfig = {
              ...responseConfig,
              headers: {
                // 拷贝 headers，避免直接复用同一对象导致意外污染。
                ...responseConfig?.headers,
              },
              // 防止重试请求再次进入 refresh 分支。
              skipAuthRefresh: true,
            }

            return service.request(retryConfig)
          })
          .catch(() => {
            // refresh 失败说明本地 token 已经不可用，清掉它，避免后续继续发错。
            localStorage.removeItem(TOKEN_KEY)
            if (shouldRedirectToUnauthorized(responseConfig)) {
              unauthorizedRedirect.toUnauthorized()
            }

            return Promise.reject(new Error(message))
          })
      }

      // 没有资格刷新，或者本来就是认证类请求时，沿用旧的未授权处理逻辑。
      localStorage.removeItem(TOKEN_KEY)
      if (shouldRedirectToUnauthorized(responseConfig)) {
        unauthorizedRedirect.toUnauthorized()
      }
    }

    return Promise.reject(new Error(message))
  }

  return Promise.reject(error)
}

// 业务实例：请求前自动挂 token，响应后统一校验业务 code。
service.interceptors.request.use(attachAuthToken, handleRequestError)
service.interceptors.response.use(handleResponse, handleResponseError)
// cookie 实例：只做响应层处理，因为它主要用于 refresh，不需要 access token 头。
cookieService.interceptors.response.use(handleResponse, handleResponseError)

// 统一请求入口。
// 支持两种调用方式：
// 1. request('/path', { method, body, ... })
// 2. request({ url, method, data, ... })
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

// 手工挂载常见 REST 方法，供业务代码直接调用。
request.get = (url, params, config = {}) => service.get(url, { ...config, params })
request.post = (url, data, config = {}) => service.post(url, data, config)
request.put = (url, data, config = {}) => service.put(url, data, config)
request.delete = (url, config = {}) => service.delete(url, config)
request.download = (url, params, config = {}) =>
  // 下载接口返回 blob，通常不走业务 code 包裹，而是直接拿原始二进制数据。
  service.get(url, { ...config, params, responseType: 'blob', rawResponse: true })

// 依赖 cookie 的请求入口，主要用于 refresh 这类接口。
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

// 透出 post 便捷方法，避免调用方直接接触底层 axios 实例。
cookieRequest.post = (url, data, config = {}) => cookieService.post(url, data, config)

// 登录成功后写入 access token。
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

// 退出登录或 token 失效时清理 access token。
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// 导出底层常量和实例，便于测试或少量高级场景复用。
export { TOKEN_KEY, cookieService, service }
