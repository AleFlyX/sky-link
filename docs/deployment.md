# 部署指南

## 推荐策略

针对此代码仓库，最实用的部署形态如下：

1. `frontend` —— 作为静态站点容器。
2. `backend/land` —— 作为 REST API 和消息 WebSocket 服务。
3. `backend/collaboration` —— 作为文档协作 WebSocket 服务。
4. `nginx` —— 作为单一公网入口。
5. `mysql` —— 作为共享数据库。

这意味着前端和后端仍然是各自可独立部署的单元，但在外部访问上，应当通过同一个反向代理策略对外暴露。

选择此方案更适合当前代码库的原因：

- 前端默认已假定 API 访问采用同源（same-origin）方式。
- 后端暴露了 `/api/v1/**` 和 `/ws/messages` 路径。
- 协作服务是一个独立进程，应当保持分离。
- 后端会显式返回协作 WebSocket 的 URL，因此公网网关路径必须保持稳定。

## 公网路由

提供的 `nginx` 配置使用以下路由：

- `/` → 前端
- `/api/v1/` → Spring Boot 后端
- `/ws/messages` → Spring Boot 消息 WebSocket
- `/collaboration` → 协作 WebSocket 服务

在生产环境中，你应在 `nginx` 处终止 TLS，并对外暴露：

- `https://你的域名/`
- `wss://你的域名/ws/messages`
- `wss://你的域名/collaboration`

然后设置：

- `SKYLINK_CORS_ALLOWED_ORIGIN_PATTERNS=https://你的域名`
- `COLLABORATION_ALLOWED_ORIGINS=https://你的域名`
- `SKYLINK_COLLABORATION_WEBSOCKET_URL=wss://你的域名/collaboration`

## Compose 使用说明

1. 复制 `deploy/.env.example` 为 `.env`。
2. 填写强密码/密钥。
3. 启动服务栈：

```bash
docker compose up -d --build
```

4. 访问 `http://localhost` 或你配置的域名。

## 注意事项

- 后端默认的 `application.yaml` 使用本地 MySQL 主机，因此 Compose 栈通过 `deploy/backend/application-docker.yaml` 进行覆盖。
- 协作服务需要 `COLLABORATION_ALLOWED_ORIGINS` 环境变量，缺少该变量将导致启动失败。
- 当前消息 WebSocket 仍使用查询字符串传递 token 进行认证。该方式可以工作，但并非生产环境的首选长期方案。