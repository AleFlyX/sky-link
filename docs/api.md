# SkyLink 团队协作办公平台 RESTful API 文档

## 1. 概述

### 1.1 基础信息
- **基础URL**：`/api/v1`
- **数据格式**：所有请求与响应均使用 `application/json`
- **字符编码**：UTF-8

### 1.2 认证方式
除注册、登录、获取公钥等接口外，所有请求均需在 HTTP Header 中携带 JWT Token：
```
Authorization: Bearer <token>
```
Token 通过登录接口获取，有效期为 24 小时，过期后需重新登录或使用刷新令牌（若实现）。

### 1.3 通用响应格式
所有接口返回统一 JSON 结构：
```json
{
  "code": 200,           // 状态码，仅 200 表示成功，其他表示错误
  "message": "success",// 提示信息
  "data": null         // 具体数据，可为对象、数组或 null
}
```
**常见错误码**：
| code | 说明                     |
|------|--------------------------|
| 200  | 成功                     |
| 400  | 请求参数错误             |
| 401  | 未认证或 Token 无效/过期 |
| 403  | 无权限访问               |
| 404  | 资源不存在               |
| 409  | 数据冲突（如重复）       |
| 500  | 服务器内部错误           |

### 1.4 分页参数
列表接口通用分页参数（Query）：
- `page`：页码，从 1 开始，默认 1
- `size`：每页条数，默认 20，最大 100

分页响应格式：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "records": [ ... ]
  }
}
```

---

## 2. 认证模块（Auth）

### 2.1 用户注册
- **接口**：`POST /auth/register`
- **描述**：新用户注册，密码需满足复杂度要求（至少8位，含字母和数字）
- **请求体**：
```json
{
  "username": "zhangsan",        // 必填，唯一
  "password": "Abc123456",       // 必填
  "nickname": "张三",            // 可选，默认为用户名
  "email": "zhangsan@example.com", // 必填，唯一
  "phone": "13800138000"        // 必填，唯一
}
```
- **响应**（成功）：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1001,
    "username": "zhangsan",
    "nickname": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "createTime": "2026-07-13T10:00:00Z"
  }
}
```

### 2.2 用户登录
- **接口**：`POST /auth/login`
- **描述**：支持用户名或邮箱登录，返回 JWT Token
- **请求体**：
```json
{
  "account": "zhangsan",         // 用户名或邮箱
  "password": "Abc123456"
}
```
- **响应**（成功）：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400,
    "userInfo": {
      "userId": 1001,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "/uploads/avatars/1001.jpg",
      "email": "zhangsan@example.com",
      "phone": "13800138000",
      "departmentId": 10,
      "status": 1,
      "roles": ["ROLE_USER"]
    }
  }
}
```

### 2.3 刷新 Token（可选）
- **接口**：`POST /auth/refresh`
- **描述**：使用 refresh_token 换取新 access_token
- **请求体**：
```json
{
  "refreshToken": "xxx"
}
```
- **响应**：同登录返回的 token 结构。

### 2.4 退出登录
- **接口**：`POST /auth/logout`
- **描述**：使当前 Token 失效（服务端可做黑名单，前端清除本地 Token）
- **请求头**：需携带 Token
- **响应**：
```json
{
  "code": 200,
  "message": "退出成功"
}
```

---

## 3. 用户管理（User）

### 3.1 获取当前用户信息
- **接口**：`GET /users/me`
- **描述**：获取当前登录用户详细信息
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "/uploads/avatars/1001.jpg",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "status": 1,
    "departmentId": 10,
    "departmentName": "研发部",
    "createTime": "2026-07-13T10:00:00Z",
    "roles": [
      { "roleId": 1, "roleName": "普通成员", "roleCode": "ROLE_USER" }
    ]
  }
}
```

### 3.2 更新当前用户信息
- **接口**：`PUT /users/me`
- **描述**：修改个人资料（昵称、头像、邮箱、手机号等）
- **请求体**（所有字段可选）：
```json
{
  "nickname": "张三丰",
  "avatar": "/uploads/avatars/1001.jpg",
  "email": "newemail@example.com",
  "phone": "13900139000"
}
```
- **响应**：返回更新后的用户信息（同 3.1）。

### 3.3 修改密码
- **接口**：`PUT /users/me/password`
- **描述**：修改当前用户密码
- **请求体**：
```json
{
  "oldPassword": "Old123",
  "newPassword": "New456"
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

### 3.4 获取用户列表（管理员）
- **接口**：`GET /users`
- **描述**：分页查询所有用户，支持筛选
- **权限**：超级管理员或管理员
- **Query 参数**：
  - `page`、`size`（通用）
  - `username`：模糊搜索用户名
  - `nickname`：模糊搜索昵称
  - `departmentId`：按部门筛选
  - `status`：按状态筛选（0禁用，1启用）
- **响应**：分页数据，records 为用户信息摘要（不含密码等敏感字段）。

### 3.5 获取指定用户信息
- **接口**：`GET /users/{userId}`
- **描述**：查看其他用户公开资料
- **路径参数**：`userId`
- **响应**：同 3.1 结构（不含敏感信息如手机号，仅展示必要字段）。

### 3.6 更新用户状态（管理员）
- **接口**：`PUT /users/{userId}/status`
- **描述**：启用或禁用用户
- **权限**：超级管理员或管理员
- **请求体**：
```json
{
  "status": 0   // 0禁用，1启用
}
```
- **响应**：返回该用户最新信息。

### 3.7 删除用户（管理员）
- **接口**：`DELETE /users/{userId}`
- **描述**：逻辑删除用户
- **权限**：超级管理员
- **响应**：
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 4. 部门管理（Department）

### 4.1 获取部门列表
- **接口**：`GET /departments`
- **描述**：获取所有部门列表（含成员数）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "departmentId": 1,
      "departmentName": "研发部",
      "leaderId": 1001,
      "leaderName": "张三",
      "description": "负责产品研发",
      "memberCount": 20
    }
  ]
}
```

### 4.2 创建部门（管理员）
- **接口**：`POST /departments`
- **请求体**：
```json
{
  "departmentName": "测试部",
  "leaderId": 1005,
  "description": "负责质量保障"
}
```
- **响应**：返回创建的部门信息。

### 4.3 更新部门信息（管理员）
- **接口**：`PUT /departments/{departmentId}`
- **请求体**：同创建，字段可选。
- **响应**：更新后的部门信息。

### 4.4 删除部门（管理员）
- **接口**：`DELETE /departments/{departmentId}`
- **描述**：仅当部门无成员时可删除
- **响应**：成功消息。

### 4.5 获取部门成员
- **接口**：`GET /departments/{departmentId}/members`
- **描述**：分页获取该部门下的所有成员
- **Query 参数**：分页参数
- **响应**：分页用户列表（简要信息）。

---

## 5. 好友管理（Friend）

### 5.1 添加好友
- **接口**：`POST /friends/requests`
- **描述**：发送好友申请
- **请求体**：
```json
{
  "friendUserId": 1002,   // 目标用户ID
  "message": "你好，我是张三"  // 可选申请附言
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "申请已发送",
  "data": {
    "requestId": 123,
    "status": "pending"
  }
}
```

### 5.2 处理好友申请
- **接口**：`PUT /friends/requests/{requestId}`
- **描述**：同意或拒绝好友申请
- **请求体**：
```json
{
  "action": "accept"   // accept 或 reject
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "已同意好友申请",
  "data": {
    "friendUserId": 1001,
    "friendUser": { ... }  // 好友信息
  }
}
```

### 5.3 获取好友列表
- **接口**：`GET /friends`
- **描述**：获取当前用户已经建立关系的所有好友
- **Query 参数**：可选分页和关键字搜索（nickname）
- **响应**：分页好友列表，每条包含好友用户信息及添加时间。

### 5.4 删除好友
- **接口**：`DELETE /friends/{friendUserId}`
- **描述**：解除好友关系
- **路径参数**：`friendUserId`（对方用户ID）
- **响应**：成功消息。

### 5.5 获取好友申请列表
- **接口**：`GET /friends/requests`
- **描述**：获取当前用户收到的待处理好友申请
- **Query 参数**：分页
- **响应**：分页申请列表，包含申请方信息、附言、申请时间。

---

## 6. 群组管理（Group）

### 6.1 创建群聊
- **接口**：`POST /groups`
- **描述**：创建群组，可指定初始成员
- **请求体**：
```json
{
  "groupName": "项目A讨论组",
  "avatar": "/uploads/avatars/1001.jpg",   // 可选
  "memberIds": [1002, 1003]  // 初始邀请成员ID列表
}
```
- **响应**：返回群组详情，包含群主信息。

### 6.2 获取群组列表
- **接口**：`GET /groups`
- **描述**：获取当前用户加入的所有群组
- **Query 参数**：分页
- **响应**：分页群组简要信息。

### 6.3 获取群组详情
- **接口**：`GET /groups/{groupId}`
- **描述**：获取群组详细信息（含群成员数量等）
- **响应**：群组完整信息。

### 6.4 更新群组信息（群主或管理员）
- **接口**：`PUT /groups/{groupId}`
- **请求体**（字段可选）：
```json
{
  "groupName": "新名字",
  "avatar": "新头像"
}
```
- **响应**：更新后的群组信息。

### 6.5 解散群组（仅群主）
- **接口**：`DELETE /groups/{groupId}`
- **描述**：解散群组，所有成员被移除
- **响应**：成功消息。

### 6.6 获取群成员列表
- **接口**：`GET /groups/{groupId}/members`
- **描述**：分页获取群成员列表
- **Query 参数**：分页
- **响应**：分页成员信息，包含角色（群主/管理员/普通成员）、加入时间。

### 6.7 邀请成员入群
- **接口**：`POST /groups/{groupId}/members`
- **描述**：群主或管理员邀请用户入群
- **请求体**：
```json
{
  "userIds": [1004, 1005]
}
```
- **响应**：返回被邀请成员列表（或成功消息）。

### 6.8 踢出群成员
- **接口**：`DELETE /groups/{groupId}/members/{userId}`
- **描述**：群主或管理员踢出成员
- **路径参数**：`groupId`, `userId`
- **响应**：成功消息。

### 6.9 设置/取消群管理员
- **接口**：`PUT /groups/{groupId}/members/{userId}/role`
- **描述**：将普通成员设为管理员，或取消管理员（仅群主）
- **请求体**：
```json
{
  "role": "admin"   // admin 或 member
}
```
- **响应**：返回更新后的成员角色。

### 6.10 退出群聊
- **接口**：`DELETE /groups/{groupId}/members/me`
- **描述**：当前用户退出群组
- **响应**：成功消息。

---

## 7. 消息管理（Message）

### 7.1 发送消息（单聊或群聊）
- **接口**：`POST /messages`
- **描述**：发送文本、图片、系统等消息
- **请求体**：
```json
{
  "receiverId": 1002,       // 单聊时接收者ID
  "groupId": null,          // 群聊时群ID，二选一
  "messageType": "text",    // text,  emoji
  "content": "Hello"        // 文本内容或图片资源路径
}
```
- **响应**：返回消息对象，包含消息ID、发送时间等。

### 7.2 获取会话列表
- **接口**：`GET /messages/sessions`
- **描述**：获取当前用户的所有会话（包括单聊和群聊），按最后消息时间排序
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "sessionType": "single",   // single 或 group
      "targetId": 1002,
      "targetName": "李四",
      "targetAvatar": "...",
      "lastMessage": { ... },
      "lastTime": "2026-07-13T15:30:00Z"
    }
  ]
}
```

### 7.3 获取历史消息
- **接口**：`GET /messages`
- **描述**：获取与指定用户或指定群的历史消息（分页）
- **Query 参数**：
  - `receiverId`：单聊对方用户ID
  - `groupId`：群聊ID
  - `before`：可选，消息ID，用于加载更早消息（游标分页）
  - `page`、`size`（普通分页，不推荐与before混用）
- **响应**：分页消息列表，按时间升序或降序。

### 7.4 撤回消息
- **接口**：`DELETE /messages/{messageId}`
- **描述**：撤回自己发送的消息（需在发送后2分钟内）
- **路径参数**：`messageId`
- **响应**：成功消息。

---

## 8. 在线文档（Document）

### 8.1 创建文档
- **接口**：`POST /documents`
- **描述**：创建 Markdown 文档
- **请求体**：
```json
{
  "title": "项目计划书",
  "content": "# 项目计划...",
  "status": "private"   // private 或 team
}
```
- **响应**：返回文档对象。

### 8.2 获取文档列表
- **接口**：`GET /documents`
- **描述**：获取当前用户可查看的文档列表（自己创建或被授权的）
- **Query 参数**：分页、`title` 搜索
- **响应**：分页文档摘要（不含完整内容）。

### 8.3 获取文档详情
- **接口**：`GET /documents/{documentId}`
- **描述**：获取文档完整内容（需权限）
- **响应**：文档完整信息，包括 content。

### 8.4 更新文档
- **接口**：`PUT /documents/{documentId}`
- **描述**：修改文档标题、内容、状态等
- **请求体**（字段可选）：
```json
{
  "title": "新标题",
  "content": "新内容...",
  "status": "team"
}
```
- **响应**：更新后的文档。

### 8.5 删除文档
- **接口**：`DELETE /documents/{documentId}`
- **描述**：逻辑删除（仅创建者或管理员）
- **响应**：成功消息。

### 8.6 设置用户文档权限
- **接口**：`PUT /documents/{documentId}/permissions/{userId}`
- **描述**：幂等地设置指定用户的文档权限
- **请求体**：
```json
{
  "permissionType": "edit"   // read, comment, edit, manage
}
```
- **响应**：返回权限记录。

### 8.7 设置群组文档权限
- **接口**：`PUT /documents/{documentId}/group-permissions/{groupId}`
- **描述**：幂等地设置指定群组的文档权限，群成员据此获得访问能力
- **请求体**：
```json
{
  "permissionType": "read"
}
```
- **响应**：返回权限记录。

### 8.8 获取文档权限列表
- **接口**：`GET /documents/{documentId}/permissions`
- **描述**：查看所有对该文档有权限的用户和群组
- **响应**：分别返回用户权限列表和群组权限列表。

### 8.9 移除用户文档权限
- **接口**：`DELETE /documents/{documentId}/permissions/{userId}`
- **描述**：删除指定用户的权限记录
- **响应**：成功消息。

### 8.10 移除群组文档权限
- **接口**：`DELETE /documents/{documentId}/group-permissions/{groupId}`
- **描述**：删除指定群组的权限记录
- **响应**：成功消息。

---

## 9. 任务管理（Task）

### 9.1 创建任务
- **接口**：`POST /tasks`
- **描述**：管理员或项目负责人创建任务
- **请求体**：
```json
{
  "title": "开发登录模块",
  "content": "实现JWT认证和注册接口",
  "executorId": 1002,        // 负责人ID
  "priority": 1,             // 1低，2中，3高
  "deadline": "2026-07-20T18:00:00Z"
}
```
- **响应**：返回任务对象。

### 9.2 获取任务列表
- **接口**：`GET /tasks`
- **描述**：获取当前用户参与的任务（创建或负责），支持筛选
- **Query 参数**：
  - `status`：未开始、进行中、已完成（可枚举）
  - `priority`：筛选优先级
  - `executorId`：按负责人筛选
  - `page`, `size`
- **响应**：分页任务列表。

### 9.3 获取任务详情
- **接口**：`GET /tasks/{taskId}`
- **响应**：任务完整信息。

### 9.4 更新任务
- **接口**：`PUT /tasks/{taskId}`
- **描述**：修改任务信息（管理员或创建者）
- **请求体**：字段同创建，可选。
- **响应**：更新后的任务。

### 9.5 更新任务状态（负责人或管理员）
- **接口**：`PUT /tasks/{taskId}/status`
- **请求体**：
```json
{
  "status": "进行中"   // 未开始 / 进行中 / 已完成
}
```
- **响应**：任务状态更新后的信息。

### 9.6 删除任务
- **接口**：`DELETE /tasks/{taskId}`
- **描述**：仅管理员或创建者
- **响应**：成功消息。

---

## 10. 系统管理（Admin）

### 10.1 角色管理

#### 10.1.1 获取角色列表
- **接口**：`GET /roles`
- **权限**：超级管理员
- **响应**：返回所有角色。

#### 10.1.2 创建角色
- **接口**：`POST /roles`
- **请求体**：
```json
{
  "roleName": "项目主管",
  "roleCode": "ROLE_PROJECT_LEADER",
  "description": "负责项目管理"
}
```
- **响应**：返回角色。

#### 10.1.3 更新角色
- **接口**：`PUT /roles/{roleId}`
- **请求体**：字段可选。
- **响应**：更新后角色。

#### 10.1.4 删除角色
- **接口**：`DELETE /roles/{roleId}`
- **响应**：成功消息。

#### 10.1.5 为角色分配权限（需扩展权限表）
- **接口**：`PUT /roles/{roleId}/permissions`
- **请求体**：
```json
{
  "permissionCodes": ["user:add", "user:update"]
}
```
- **响应**：成功。

### 10.2 权限管理（仅超管）
#### 10.2.1 获取所有权限列表
- **接口**：`GET /permissions`
- **响应**：权限树或列表。

### 10.3 用户角色分配（管理员）
#### 10.3.1 为用户分配角色
- **接口**：`POST /users/{userId}/roles`
- **请求体**：
```json
{
  "roleIds": [1, 2]
}
```
- **响应**：返回用户最新角色列表。

#### 10.3.2 移除用户角色
- **接口**：`DELETE /users/{userId}/roles/{roleId}`
- **响应**：成功。

### 10.4 系统配置（可选）
- 暂不详细设计，可预留 `GET /system/config` 和 `PUT /system/config` 用于超管修改系统参数。

---

## 附录 A：枚举值说明

| 字段                 | 枚举值                                                     |
|----------------------|------------------------------------------------------------|
| 用户状态 (status)    | 0-禁用，1-启用                                             |
| 消息类型 (messageType) | text, image, system, emoji                              |
| 任务状态 (status)    | 未开始, 进行中, 已完成                                    |
| 任务优先级 (priority) | 1-低，2-中，3-高                                           |
| 文档权限 (permissionType) | read, comment, edit, manage                           |
| 好友申请状态 (status) | pending, accepted, rejected                              |
| 群成员角色 (role)    | owner, admin, member                                     |

---

**文档版本**：v1.0  
**最后更新**：2026-07-13  
**维护人**：SkyLink 开发团队
