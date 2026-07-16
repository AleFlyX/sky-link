# 配置说明 README

## 适用范围

这份文档用于说明当前项目里以下几类地址和配置分别在哪里定义、由谁控制、部署时应该怎么填：

- 前端 REST 请求基础地址
- 前端消息 WebSocket 地址
- 前端协同编辑 WebSocket 地址
- 后端返回给前端的协同服务地址
- 协同服务回调 Spring Boot 的内部鉴权地址
- Docker Compose 部署里 Nginx 的统一代理策略

这份文档主要面向部署、联调和排查配置问题。

## 先看整体链路

当前仓库里其实有两条不同的 WebSocket 链路：

1. 消息中心 WebSocket
   - 浏览器直接连接 Spring Boot 的 `/ws/messages`
   - 地址由前端本地拼出来

2. 文档协同 WebSocket
   - 浏览器先向 Spring Boot 请求协同票据
   - Spring Boot 返回 `token + websocketUrl`
   - 前端再拿这个 `websocketUrl` 去连接 collaboration 服务

所以：

- 消息 WebSocket 地址主要由前端环境和当前页面域名决定
- 协同 WebSocket 地址主要由后端配置决定

这两者不要混为一谈。

## 1. 前端 REST 请求地址

前端 REST 请求基础地址定义在：

[frontend/sky-link-frontend/src/utils/request.js](/E:/A%20study/A软件工程/sky-link/frontend/sky-link-frontend/src/utils/request.js:6)

```js
baseURL: import.meta.env.VITE_API_BASE_URL || ''
```

含义是：

- 如果配置了 `VITE_API_BASE_URL`，前端 REST 请求就走这个地址
- 如果没配置，前端就走同源相对路径

常见用法：

- 本地开发走 Vite 代理：通常不填
- 前后端分域部署：可填 `https://api.example.com/api/v1`
- 统一 Nginx 反代：通常也不需要显式填写，走同源即可

## 2. 前端消息 WebSocket 地址

前端消息 WebSocket 地址构造逻辑在：

[frontend/sky-link-frontend/src/views/messages/composables/useMessageCenter.js](/E:/A%20study/A软件工程/sky-link/frontend/sky-link-frontend/src/views/messages/composables/useMessageCenter.js:62)

核心逻辑如下：

```js
const explicitWebSocketUrl = import.meta.env.VITE_WS_URL
const apiBase = explicitWebSocketUrl || import.meta.env.VITE_API_BASE_URL || window.location.origin
const url = new URL(apiBase, window.location.origin)
url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
url.pathname = explicitWebSocketUrl ? url.pathname : '/ws/messages'
url.searchParams.set('token', token)
```

它的含义是：

- 如果设置了 `VITE_WS_URL`，前端就直接连这个地址
- 如果没设置 `VITE_WS_URL`，就尝试基于 `VITE_API_BASE_URL` 推导
- 如果两者都没设置，就用当前页面域名，默认连 `/ws/messages`

也就是说，消息 WebSocket 主要受两个前端变量影响：

- `VITE_WS_URL`
- `VITE_API_BASE_URL`

推荐生产策略：

- 尽量不要单独暴露一个消息 WebSocket 域名
- 优先统一走同一个公网域名，由 Nginx 转发 `/ws/messages`

推荐写法：

```env
VITE_API_BASE_URL=
VITE_WS_URL=
```

配合公网入口：

- `https://your-domain/api/v1/...`
- `wss://your-domain/ws/messages`

## 3. 前端协同编辑 WebSocket 地址

前端协同编辑不是从前端环境变量直接读取地址，而是使用后端票据接口返回的地址。

前端使用位置在：

[frontend/sky-link-frontend/src/views/documents/composables/useCollaborationSession.js](/E:/A%20study/A软件工程/sky-link/frontend/sky-link-frontend/src/views/documents/composables/useCollaborationSession.js:27)

```js
provider.value = new HocuspocusProvider({
  url: firstTicket.websocketUrl,
```

票据请求位置在：

[frontend/sky-link-frontend/src/api/document.js](/E:/A%20study/A软件工程/sky-link/frontend/sky-link-frontend/src/api/document.js:47)

```js
return request.post(`/documents/${documentId}/collaboration-ticket`)
```

这意味着：

- 前端自己不决定协同服务地址
- 前端只是请求票据
- 真正返回给浏览器的协同地址由 Spring Boot 决定

## 4. 后端返回给前端的协同地址

Spring Boot 返回协同 WebSocket 地址的位置在：

[backend/land/src/main/java/com/skylink/land/service/document/DocumentCollaborationService.java](/E:/A%20study/A软件工程/sky-link/backend/land/src/main/java/com/skylink/land/service/document/DocumentCollaborationService.java:36)

```java
.websocketUrl(properties.getWebsocketUrl())
```

这个配置来自：

[backend/land/src/main/resources/application.yaml](/E:/A%20study/A软件工程/sky-link/backend/land/src/main/resources/application.yaml:69)

```yaml
skylink:
  collaboration:
    websocket-url: ${SKYLINK_COLLABORATION_WEBSOCKET_URL:ws://127.0.0.1:8180}
```

真正控制前端协同连接地址的核心变量是：

- `SKYLINK_COLLABORATION_WEBSOCKET_URL`

如果按统一 Nginx 代理的生产方案，推荐写成：

```env
SKYLINK_COLLABORATION_WEBSOCKET_URL=wss://your-domain/collaboration
```

注意这里必须是“浏览器可访问的公网地址”，不能写成容器内部地址，例如：

- `ws://collaboration:8180` 这种写法不对

因为浏览器根本访问不到 Docker 内部服务名。

## 5. Spring Boot 的 CORS 和消息 WebSocket Origin 控制

Spring Boot HTTP CORS 配置在：

[backend/land/src/main/java/com/skylink/land/config/WebMvcConfiguration.java](/E:/A%20study/A软件工程/sky-link/backend/land/src/main/java/com/skylink/land/config/WebMvcConfiguration.java:41)

Spring Boot 消息 WebSocket 的 Origin 限制在：

[backend/land/src/main/java/com/skylink/land/config/WebSocketConfiguration.java](/E:/A%20study/A软件工程/sky-link/backend/land/src/main/java/com/skylink/land/config/WebSocketConfiguration.java:32)

这两处都依赖同一个配置：

- `SKYLINK_CORS_ALLOWED_ORIGIN_PATTERNS`

生产环境推荐值：

```env
SKYLINK_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-domain
```

如果允许多个来源，可以用逗号分隔。

## 6. collaboration 服务自身配置

collaboration 服务读取配置的位置在：

[backend/collaboration/src/config.js](/E:/A%20study/A软件工程/sky-link/backend/collaboration/src/config.js:13)

关键变量包括：

- `COLLABORATION_HOST`
- `COLLABORATION_PORT`
- `COLLABORATION_ALLOWED_ORIGINS`
- `SKYLINK_COLLABORATION_TICKET_SECRET`
- `SKYLINK_COLLABORATION_SERVICE_TOKEN`
- `SKYLINK_INTERNAL_BASE_URL`

它们的作用分别是：

- `COLLABORATION_HOST` 和 `COLLABORATION_PORT`：决定协同服务监听在哪个地址和端口
- `COLLABORATION_ALLOWED_ORIGINS`：决定哪些浏览器来源允许建立协同 WebSocket 连接
- `SKYLINK_INTERNAL_BASE_URL`：决定 collaboration 服务向谁发内部鉴权请求

推荐 Docker 部署写法：

```env
COLLABORATION_HOST=0.0.0.0
COLLABORATION_PORT=8180
COLLABORATION_ALLOWED_ORIGINS=https://your-domain
SKYLINK_INTERNAL_BASE_URL=http://backend:8080
```

这里要特别区分两类地址：

- `SKYLINK_INTERNAL_BASE_URL`：服务之间内部通信地址
- `SKYLINK_COLLABORATION_WEBSOCKET_URL`：浏览器访问的公网地址

这两个不能写反。

## 7. Nginx 统一代理策略

当前我补的反向代理文件在：

[deploy/nginx/nginx.conf](/E:/A%20study/A软件工程/sky-link/deploy/nginx/nginx.conf:33)

它定义的公网入口是：

- `/` -> 前端静态站点
- `/api/v1/` -> Spring Boot REST 接口
- `/ws/messages` -> Spring Boot 消息 WebSocket
- `/collaboration` -> collaboration 协同服务

这也是当前项目更推荐的部署方式：

- 前端、后端、协同服务各自独立运行
- 对外只暴露一个统一域名
- 由 Nginx 按路径转发

## 8. 当前项目推荐的生产部署策略

结合当前代码结构，更推荐下面这种策略：

1. 前端单独构建为静态站点容器
2. `backend/land` 单独部署，负责 REST 和消息 WebSocket
3. `backend/collaboration` 单独部署，负责文档协同
4. MySQL 单独部署
5. Nginx 作为唯一公网入口，统一处理 HTTPS、WSS 和反向代理

也就是说：

- 内部部署层面是“前后端分开部署”
- 外部访问层面是“统一入口策略”

这比“前端一个域名、API 一个域名、协同再一个域名”更容易控制：

- 跨域
- WebSocket 升级
- HTTPS 证书
- 日志和审计
- 后续扩容和替换

## 9. 建议的公网访问形态

推荐最终对外暴露成这样：

- `https://your-domain/`
- `https://your-domain/api/v1/...`
- `wss://your-domain/ws/messages`
- `wss://your-domain/collaboration`

对应最关键的生产环境变量至少应包括：

```env
JWT_SECRET=replace-with-a-random-secret-longer-than-32-bytes
SKYLINK_COLLABORATION_TICKET_SECRET=replace-with-another-random-secret-longer-than-32-bytes
SKYLINK_COLLABORATION_SERVICE_TOKEN=replace-with-a-service-token-longer-than-32-bytes
SKYLINK_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-domain
COLLABORATION_ALLOWED_ORIGINS=https://your-domain
SKYLINK_COLLABORATION_WEBSOCKET_URL=wss://your-domain/collaboration
SKYLINK_INTERNAL_BASE_URL=http://backend:8080
```

## 10. 当前已知注意事项

当前消息 WebSocket 仍然把 JWT 放在 URL 查询参数里。

这和文档协同的“短期票据”方案相比，规范性和安全性都弱一些。现在代码可以工作，但从长期来看，更推荐后续把消息 WebSocket 也改成和协同服务一致的短票据模式。
