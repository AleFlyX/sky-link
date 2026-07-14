# SkyLink

SkyLink 是一个面向企业团队、校园组织和项目团队的轻量级协同办公平台。项目希望把组织管理、即时沟通、任务与日程、在线文档、文件共享和公告通知集中到同一套系统中，减少信息分散和多工具切换带来的协作成本。

## 当前状态

项目目前处于持续开发和前后端联调阶段：

- 前端已经搭建登录、工作台、个人中心、用户、部门、通讯录、消息、文件、文档、任务、日程和公告等主要页面，并提供演示数据与接口降级能力。
- 后端已经建立统一响应、异常处理、JWT 鉴权、MyBatis-Plus 数据访问等基础能力，并实现认证、当前用户和好友关系等首批接口。
- `docs/` 中描述的是系统完整目标范围；部分业务接口仍在逐步落地，请以代码和接口文档的最新状态为准。

## 核心能力

- **身份与权限**：注册登录、JWT 身份认证、个人资料、用户与部门管理、RBAC 角色权限。
- **团队沟通**：通讯录、好友关系、单聊、群聊、消息已读与撤回。
- **内容协作**：Markdown 文档、协作权限、收藏、文件上传下载与共享。
- **工作管理**：任务分配、优先级、进度、附件、个人与团队日程。
- **信息触达**：公告、通知、活动以及未读状态管理。
- **系统治理**：登录日志、操作日志、文件日志、删除日志、系统配置与数据统计。

## 技术栈

| 区域 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 8、Vue Router、Pinia、Element Plus、Axios |
| 前端质量 | Vitest、Vue Test Utils、ESLint、Oxlint、Prettier |
| 后端 | Java 17、Spring Boot 4.1、Spring MVC、MyBatis-Plus 3.5.9 |
| 安全 | JWT、BCrypt、基于角色的权限模型 |
| 数据库 | MySQL、InnoDB、utf8mb4 |

## 项目结构

```text
sky-link/
├── docs/                         # 需求、模型、接口、SQL 与协作计划
├── frontend/
│   └── sky-link-frontend/        # Vue 3 + Vite 前端
│       ├── public/
│       └── src/
│           ├── api/              # 业务 API 封装
│           ├── components/       # 通用组件
│           ├── layouts/          # 应用布局
│           ├── mock/             # 演示与降级数据
│           ├── router/           # 路由配置
│           ├── stores/           # Pinia 状态
│           ├── utils/            # 请求等基础工具
│           └── views/            # 业务页面
└── backend/
    └── land/                     # Spring Boot 后端
        └── src/
            ├── main/java/com/skylink/land/
            │   ├── auth/         # JWT 与认证上下文
            │   ├── config/       # Web 与 MyBatis-Plus 配置
            │   ├── controller/   # REST 接口
            │   ├── dto/          # 请求与响应 DTO
            │   ├── entity/       # 领域实体
            │   ├── mapper/       # MyBatis-Plus Mapper
            │   ├── service/      # 业务服务
            │   └── web/          # 统一响应与异常处理
            └── test/             # 后端测试
```

## 本地运行

### 环境要求

- Node.js `^22.18.0` 或 `>=24.12.0`
- npm
- JDK 17
- MySQL 8.x

### 1. 初始化数据库

先在 MySQL 中创建开发数据库：

```sql
CREATE DATABASE skylink
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE skylink;
```

数据库创建完成后不需要手动建表。后端启动时会自动执行 `src/main/resources/schema.sql`，以 `CREATE TABLE IF NOT EXISTS` 幂等创建当前需求范围内的 17 张核心表，不会删除已有表或数据。数据库本身仍需提前创建。仓库中其他实体属于需求缩减后的保留代码，不参与自动建表。

[docs/backend/sql.md](docs/backend/sql.md) 保留为数据模型参考，其中包含重建表用的 `DROP TABLE IF EXISTS`，请勿直接用于保存重要数据的环境。

### 2. 启动后端

后端默认连接 `localhost:3306/skylink`，默认端口为 `8080`。数据库凭据和 JWT 密钥没有默认值，启动前必须显式设置：

| 环境变量 | 说明 | 是否必填 |
| --- | --- | --- |
| `DB_USERNAME` | MySQL 用户名 | 是 |
| `DB_PASSWORD` | MySQL 密码 | 是 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字节，不能使用示例值 | 是 |

PowerShell 示例：

```powershell
cd backend/land
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
$jwtBytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

应用启动时会幂等创建 `ROLE_USER`、`ROLE_ADMIN`、`ROLE_SUPER_ADMIN`、当前接口权限及角色权限关系。新注册用户会自动获得 `ROLE_USER`；如果初始化数据缺失，注册事务会回滚，不会产生无角色账号。

首次部署可通过以下环境变量显式创建超级管理员。管理员成功创建后，应删除这些环境变量，并保持 `SKYLINK_BOOTSTRAP_ADMIN_ENABLED=false`：

```powershell
$env:SKYLINK_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:SKYLINK_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:SKYLINK_BOOTSTRAP_ADMIN_PASSWORD = "replace-with-a-strong-password"
$env:SKYLINK_BOOTSTRAP_ADMIN_NICKNAME = "System Admin"
$env:SKYLINK_BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:SKYLINK_BOOTSTRAP_ADMIN_PHONE = "13800000000"
```

引导密码至少 12 个字符并同时包含字母和数字。系统不会覆盖已有管理员的密码，也不会把第一个注册用户自动提升为管理员。

启动后可访问健康检查：`GET http://localhost:8080/api/v1/health`。

### 3. 启动前端

```powershell
cd frontend/sky-link-frontend
npm install
npm run dev
```

浏览器访问 Vite 终端输出的地址。当前开发配置使用端口 `5573`。

前端通过 `VITE_API_BASE_URL` 判断是否启用远程接口：未配置时使用本地演示数据，配置后请求后端，并在多数业务页面请求失败时回退到演示数据。环境变量模板见 `frontend/sky-link-frontend/.env.template`。

## 接口约定

- API 统一前缀：`/api/v1`
- JWT 请求头：`Authorization: Bearer <token>`
- 成功响应通常使用统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 分页接口统一返回当前页、每页数量、总数和记录列表。
- 完整资源、字段和状态码约定见 [docs/api.md](docs/api.md)。

## 常用命令

前端命令在 `frontend/sky-link-frontend/` 下执行：

```powershell
npm run dev
npm run build
npm run test:unit
npm run lint
npm run format
```

后端命令在 `backend/land/` 下执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

## 项目文档

- [需求分析](docs/spec.md)：项目目标、角色、功能与非功能需求。
- [数据模型](docs/model.md)：26 张核心表的设计原则、实体关系与约束。
- [接口文档](docs/api.md)：REST API、认证、分页、错误码与枚举约定。
- [数据库脚本](docs/backend/sql.md)：与需求和模型对齐的 MySQL DDL。
- [后端主线 A 计划](docs/person-a-backend-plan.md)：认证、用户、组织与权限。
- [后端主线 B 计划](docs/person-b-backend-plan.md)：聊天、文件、任务等协作业务。
- [全栈联调计划](docs/person-c-fullstack-plan.md)：前端页面、公共组件与接口联调。

## 开发约定

- 前端 JavaScript/Vue 使用 2 空格缩进，Java 使用 4 空格缩进。
- Vue 组件和 Java 类使用 `PascalCase`，变量与方法使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。
- 行为、校验或实体映射发生变化时，应同步补充对应测试。
- 调整实体或 SQL 时，同时更新 `docs/spec.md`、`docs/model.md` 和 `docs/backend/sql.md`，保持需求、模型与实现一致。

## License

本项目基于 [MIT License](LICENSE) 开源。
