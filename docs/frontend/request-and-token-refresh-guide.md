# 前端请求拦截与 Token 自动续期（小白版）

前端每次请求不需要业务页面手写 Token。统一请求模块会自动带 access token；如果它过期而后端返回 401，前端会利用浏览器自动携带的 refresh cookie 换一个新 token，再把刚才失败的请求重发一次。

后端签发与验证 JWT 的原理请配合阅读：[JWT 教程](../backend/jwt-guide.md)。

## 完整流程

```text
页面调用 request.get('/api/...')
  -> 请求拦截器从 localStorage 读 access token
  -> Authorization: Bearer <token>
  -> 后端返回 { code: 200, data: ... }，前端返回 payload

若后端 HTTP 状态为 401：
  -> 多个失败请求共用同一次 POST /auth/refresh
  -> 浏览器携带 httpOnly refresh cookie
  -> 收到新 access token，写回 localStorage
  -> 重放原请求
  -> 若刷新失败，清 token 并跳转 /401
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| Axios 实例、拦截器、刷新和重试 | `frontend/sky-link-frontend/src/utils/request.js:1-240` |
| 登录后的用户状态 | `frontend/sky-link-frontend/src/stores/user.js:115-154` |
| 路由的 token 检查 | `frontend/sky-link-frontend/src/router/index.js:34-69` |

## 两个 Axios 实例为什么要分开

代码位置：`request.js:10-22`

```js
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '', // 1. 所有普通业务 API 共用的基础地址。
  timeout: 10000, // 2. 超过 10 秒还没有响应，就让调用方知道请求失败。
})

const cookieService = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '', // 3. 地址仍然相同。
  timeout: 10000,
  withCredentials: true, // 4. 关键：允许浏览器自动携带跨请求的 cookie。
})
```

`service` 用 access token；`cookieService` 主要用于刷新 Token。refresh cookie 若设置为 `httpOnly`，JavaScript 看不到它，但浏览器仍能按规则带给后端，这正是它比把 refresh token 放 localStorage 更安全的原因。

## 请求前自动带 Token

代码位置：`request.js:28-40`，注册位置 `request.js:176-180`

```js
function attachAuthToken(config) {
  const token = localStorage.getItem(TOKEN_KEY) // 1. 每次请求都读取最新 token。

  if (token) {
    config.headers = config.headers || {} // 2. 防止 headers 不存在时赋值报错。
    config.headers.Authorization = `Bearer ${token}` // 3. 形成后端 JWT 常用格式。
  }

  return config // 4. 必须返回配置对象，Axios 才会继续发送请求。
}

service.interceptors.request.use(attachAuthToken, handleRequestError)
```

业务页面只要写 `request.get('/api/v1/tasks')`，不用重复拼 `Authorization` 头。

## 后端成功响应如何被识别

代码位置：`request.js:66-84`

```js
function handleResponse(response) {
  const payload = response.data // 1. Axios 把后端响应体放在 data 中。

  if (response.config.rawResponse) { // 2. 下载 blob 不是普通 JSON，走特殊分支。
    if (response.status !== 200) return Promise.reject(new Error('Unexpected response status'))
    return payload
  }

  if (!payload || typeof payload !== 'object' || payload.code !== 200) {
    // 3. HTTP 成功不代表业务成功；项目约定 code=200 才是成功。
    return Promise.reject(new Error(payload?.message || 'Invalid response'))
  }
  return payload // 4. 页面最终拿到 { code, message, data }。
}
```

这与后端统一响应机制是一套约定，详见 [统一响应与异常教程](../backend/global-exception-response-guide.md)。

## 401 后的刷新锁：为什么不会刷十次

代码位置：`request.js:24-26`、`94-122`

```js
let refreshAccessTokenPromise = null // 1. 空表示当前没有刷新动作。

function refreshAccessToken() {
  if (!refreshAccessTokenPromise) { // 2. 只有第一个遇到 401 的请求会真的发 refresh。
    refreshAccessTokenPromise = cookieService
      .post('/auth/refresh', undefined, {
        skipAuthRefresh: true, // 3. refresh 自己失败时不能再次刷新，否则无限循环。
      })
      .then((response) => {
        const nextToken = extractAccessToken(response) // 4. 兼容 accessToken 或 token 字段。
        if (!nextToken) throw new Error('Refresh response did not include an access token')
        setToken(nextToken) // 5. 将新 access token 保存，后续请求自动使用它。
        return nextToken
      })
      .finally(() => {
        refreshAccessTokenPromise = null // 6. 不论成功或失败都释放锁。
      })
  }
  return refreshAccessTokenPromise // 7. 后来的请求等待同一个 Promise。
}
```

例如页面同时发出 5 个请求，旧 access token 刚好过期：5 个请求可能都收到 401，但这里只会请求一次 `/auth/refresh`。

## 刷新成功后如何重放原请求

代码位置：`request.js:124-174`

```js
if (error.response?.status === 401) { // 1. 只处理 HTTP 401。
  if (!responseConfig?.skipAuthRefresh && shouldRedirectToUnauthorized(responseConfig)) {
    return refreshAccessToken().then(() => {
      const retryConfig = {
        ...responseConfig, // 2. 保留原 URL、方法、参数、请求体。
        headers: { ...responseConfig?.headers }, // 3. 拷贝 headers，避免污染旧对象。
        skipAuthRefresh: true, // 4. 重试仍然 401 时，不允许无限刷新。
      }
      return service.request(retryConfig) // 5. 再发一次原来的业务请求。
    }).catch(() => {
      localStorage.removeItem(TOKEN_KEY) // 6. refresh 也失败，旧 token 已无意义。
      unauthorizedRedirect.toUnauthorized() // 7. 进入 /401 页面。
      return Promise.reject(new Error(message))
    })
  }
}
```

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 所有请求都用 refresh cookie | 普通业务请求使用 access token；刷新才使用 cookie 实例。 |
| 401 一定说明没有权限 | 这里 401 主要按认证失效处理；403 才通常表示已登录但无权限。 |
| 刷新成功后页面要手工重试 | 不需要，模块自动重放原请求。 |
| refresh 失败还会一直尝试 | 不会，`skipAuthRefresh` 防止递归。 |
| localStorage 里有用户信息就等于接口安全 | 不等于；后端仍必须验证 JWT 和权限。 |

## 人话复盘

这个模块像前端的“门卫和续期员”：出门前自动带证件；证件过期时集中换一次新证件；换不到就清理本地状态并引导到未授权页。真正的安全判断仍在后端，前端负责让调用过程稳定、少重复代码。
