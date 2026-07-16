# land 后端配置说明

这个 README 只说明后端 `backend/land` 的运行配置，不涉及业务接口设计。

## 1. 配置文件位置

- 主配置文件：`src/main/resources/application.yaml`
- 本地模板：`src/main/resources/application-local.yaml.example`
- Docker 模板：`../../deploy/.env.example`
- 数据库初始化脚本：`src/main/resources/schema.sql`

现在建议这样使用：

- `application.yaml` 放基础配置和必须的环境变量占位
- `application-local.yaml.example` 作为本地调试模板
- `deploy/.env.example` 作为 Docker Compose 部署模板
- 你自己本地实际使用时，新建 `application-local.yaml`，不要提交真实密码和密钥

## 2. 当前配置项说明

### 服务端口

```yaml
server:
  port: 8080
```

如果前端或本机其他服务已经占用了 `8080`，这里需要改成其他端口，比如 `8081`。

### 数据库

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/skylink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

要求：

- 本机必须安装并启动 MySQL
- 必须存在名为 `skylink` 的数据库
- 必须提供环境变量 `DB_USERNAME`
- 必须提供环境变量 `DB_PASSWORD`

### JWT

```yaml
skylink:
  jwt:
    secret: ${JWT_SECRET}
```

要求：

- 必须提供环境变量 `JWT_SECRET`
- 这个值不能留空
- 长度至少 32 字节

如果不满足，上线前校验会直接阻止应用启动。

### 协同文档

```yaml
skylink:
  collaboration:
    ticket-secret: ${SKYLINK_COLLABORATION_TICKET_SECRET:}
    websocket-url: ${SKYLINK_COLLABORATION_WEBSOCKET_URL:ws://127.0.0.1:8180}
    service-token: ${SKYLINK_COLLABORATION_SERVICE_TOKEN:}
```

说明：

- 这三个配置不会阻止主应用启动
- 但只要你调用在线协同文档相关接口，如果 `ticket-secret` 或 `service-token` 没配好，就会运行时报错
- `ticket-secret` 和 `service-token` 都建议使用 32 位以上随机字符串

### 初始化 SQL

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

说明：

- 每次启动都会尝试执行 `schema.sql`
- 当前脚本大多使用 `CREATE TABLE IF NOT EXISTS`
- 适合开发期快速起库
- 不适合作为长期正式环境迁移方案

## 3. 推荐的本地修改方式

### 方式一：只改环境变量

适合你现在这种已经有 `application.yaml` 的项目。

PowerShell 示例：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="replace-with-a-random-secret-longer-than-32-bytes"
$env:SKYLINK_COLLABORATION_TICKET_SECRET="replace-with-another-random-secret"
$env:SKYLINK_COLLABORATION_SERVICE_TOKEN="replace-with-a-service-token"
```

然后再启动后端。

### 方式二：使用本地 profile 模板

如果你不想每次都手动设置环境变量，可以基于仓库里的模板新建本地专用配置文件：

1. 复制 `src/main/resources/application-local.yaml.example`
2. 重命名为 `src/main/resources/application-local.yaml`
3. 把里面的数据库、JWT、协同密钥改成你自己的值

说明：

- 模板里已经补了 `sql.init`、本地 SQL 日志和常见前端 CORS 地址
- `refresh-cookie.domain` 默认留空，适合本地 `localhost`
- `bootstrap.admin.enabled` 默认还是 `false`，避免本地误创建管理员账号

启动时激活：

```bash
--spring.profiles.active=local
```

注意：

- 不要把真实密码和真实密钥提交到仓库
- 如果这个文件只给自己本地用，建议不要提交

## 4. 启动前检查清单

启动前至少确认下面几项：

1. MySQL 已启动
2. `skylink` 或你本地配置里指定的数据库已创建
3. `DB_USERNAME` 和 `DB_PASSWORD` 已提供
4. `JWT_SECRET` 已提供，且长度足够
5. 如果要调试协同文档，`SKYLINK_COLLABORATION_TICKET_SECRET` 和 `SKYLINK_COLLABORATION_SERVICE_TOKEN` 已提供
6. 端口 `8080` 没有被其他程序占用

## 5. 当前配置为什么不可行

结合当前仓库里的 `application.yaml` 和配置类，主要有这几个问题：

### 1. 数据库用户名和密码没有默认值

当前配置写的是：

```yaml
username: ${DB_USERNAME}
password: ${DB_PASSWORD}
```

这意味着如果你的系统环境里没有这两个变量，Spring 在读取配置时就会失败，应用无法正常启动。

### 2. JWT 密钥是强制项，而且有长度校验

`JwtProperties` 在启动时会校验 `JWT_SECRET`：

- 不能为空
- 不能使用示例值
- 至少 32 字节

所以即使数据库配置正确，只要 `JWT_SECRET` 没配，应用一样起不来。

### 3. 当前配置把运行环境写死在本机 MySQL

当前数据库 URL 固定为：

```yaml
jdbc:mysql://localhost:3306/skylink...
```

这要求你的机器必须满足以下条件：

- 装了 MySQL
- MySQL 正在运行
- 3306 可访问
- 存在 `skylink` 库

只要其中一个条件不满足，当前配置就不可用。

### 4. 协同文档配置默认是空的

虽然这部分不会阻止主应用启动，但一旦进入在线协同文档相关流程：

- `ticket-secret` 为空会导致票据签发失败
- `service-token` 为空会导致内部重授权失败

也就是说，文档协同功能在当前默认配置下并不能真正使用。

### 5. 当前项目缺少“开箱即用”的本地配置层

之前仓库里只有一个 `application.yaml`，没有现成的：

- `application-local.yaml`
- 复制后即可使用的 `application-local.yaml.example`
- 可复制为根目录 `.env` 的 `deploy/.env.example`

这会导致新同学接手时，不知道必须补哪些变量，也不知道应该改哪里才安全。

## 6. 建议的后续整理方向

现在仓库已经补了 `application-local.yaml.example` 和 `deploy/.env.example`。如果你后面继续整理后端配置，建议按这个方向做：

1. 保留 `application.yaml` 作为无敏感信息的基础配置
2. 把 `application-local.yaml.example` 作为共享模板继续维护
3. 把 `deploy/.env.example` 作为 Docker 部署唯一变量入口继续维护
4. 把数据库、JWT、协同文档密钥全部改成“文档可见、实际值外置”
5. 开发环境保留 `spring.sql.init.mode=always`
6. 后续如果进入多人协作或部署阶段，再引入正式的数据库迁移工具
