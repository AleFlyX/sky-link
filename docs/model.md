# SkyLink 团队协作办公平台数据模型设计

## 1. 设计原则

SkyLink 面向企业团队、校园组织及项目团队，覆盖用户管理、组织架构、即时通讯、任务协作、在线文档与基础配置等核心场景。数据库采用 MySQL 关系模型设计，并遵循以下原则：

1. 满足第三范式（3NF），降低冗余并保证数据一致性。
2. 主业务实体使用代理主键，统一采用 `BIGINT`。
3. 多对多关联表优先使用复合主键，而不是额外增加自增 ID。
4. 配置型实体保留稳定业务编码，如 `role_code`、`permission_code`、`config_key`。
5. 业务数据优先逻辑删除，不依赖级联物理删除。
6. 枚举状态尽量使用数值码值，减少自由文本带来的维护成本。
7. `spec.md` 中出现的功能点尽量都有对应的数据承载表或明确说明由现有表承接。

---

## 2. 数据库总体结构

SkyLink 数据库共设计 **17 张核心数据表**：

```text
SkyLink Database
│
├── 用户与权限模块
│   ├── 用户(User)
│   ├── 角色(Role)
│   ├── 权限(Permission)
│   ├── 用户角色(UserRole)
│   └── 角色权限(RolePermission)
│
├── 组织架构模块
│   └── 部门(Department)
│
├── 即时通讯模块
│   ├── 好友申请(FriendRequest)
│   ├── 好友关系(Friendship)
│   ├── 群聊(ChatGroup)
│   ├── 群成员(GroupMember)
│   └── 消息(Message)
│
├── 在线文档模块
│   ├── 文档(Document)
│   ├── 文档权限(DocumentPermission)
│   ├── 文档群组权限(DocumentGroupPermission)
│   └── 文档收藏(DocumentFavorite)
│
├── 任务协作模块
│   └── 任务(Task)
│
└── 系统管理模块
    └── 系统配置(SystemConfig)
```

---

## 3. 主键设计规范

### 3.1 适合使用自增主键的表

以下表属于主业务实体，使用 `BIGINT AUTO_INCREMENT` 作为代理主键是合理的：

- `user`
- `department`
- `role`
- `permission`
- `friend_request`
- `chat_group`
- `message`
- `document`
- `task`
- `system_config`

这些表的数据记录本身具有独立生命周期，使用单列主键更方便被外部表引用。

### 3.2 不适合使用自增主键的表

以下表更适合使用复合主键或业务主键：

1. `user_role`
   主键为 `(user_id, role_id)`，表示某用户拥有某角色。

2. `role_permission`
   主键为 `(role_id, permission_id)`，表示某角色拥有某权限。

3. `group_member`
   主键为 `(group_id, user_id)`，表示某用户加入某群。

4. `document_permission`
   主键为 `(document_id, user_id)`，表示某用户拥有某文档权限。

5. `document_group_permission`
   主键为 `(document_id, group_id)`，表示某群组拥有某文档权限。

6. `friendship`
   好友关系属于对称关系，主键应为 `(user_id, friend_user_id)`，并约束 `user_id < friend_user_id`，避免 A-B 与 B-A 重复。

7. `document_favorite`
   主键为 `(user_id, document_id)`，表示某用户收藏某文档。

---

## 4. 核心实体说明

### 4.1 用户（User）

用户是系统核心实体，一个用户可以：

- 属于一个部门
- 拥有多个角色
- 添加多个好友
- 创建多个群聊
- 加入多个群聊
- 创建多个任务
- 接收多个任务
- 创建多个文档
- 收藏文档

主要字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(255) | 密码哈希 |
| email | VARCHAR(100) | 邮箱，唯一 |
| phone | VARCHAR(20) | 手机号，唯一 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 |
| status | TINYINT | 状态码 |
| department_id | BIGINT | 所属部门 |

对应 SQL 设计说明：

- `username`、`email`、`phone` 都设置唯一约束，分别承接用户名登录、邮箱登录和注册防重。
- `department_id` 关联 `department.department_id`，用于通讯录和组织管理。
- `status` 用于控制账号启用与禁用，便于管理员停用成员账号。
- `is_deleted` 用于逻辑删除，避免直接物理删除用户后影响历史业务数据。

### 4.2 部门（Department）

用于组织团队结构，一个部门可包含多个用户，并可指定一个负责人。

对应 SQL 设计说明：

- `department_name` 设置唯一约束，避免出现同名部门。
- `leader_id` 关联 `user.user_id`，表示部门负责人。
- 部门与用户是一对多关系。
- `description` 用于存储部门职责、介绍等扩展说明。

### 4.3 角色（Role）与权限（Permission）

系统采用 RBAC 模型：

- 用户与角色：多对多
- 角色与权限：多对多

因此需要两张关联表：

- `user_role`
- `role_permission`

其中：

- `role_code` 应唯一，如 `ROLE_ADMIN`
- `permission_code` 应唯一，如 `user:add`

对应 SQL 设计说明：

- `role` 表保存角色元数据，`role_name` 用于显示，`role_code` 用于程序判断。
- `permission` 表保存权限点，`permission_type` 区分菜单、按钮、接口。
- `permission.parent_id` 用于构建树形权限结构，支持菜单层级。
- `permission.sort_no` 用于前端菜单展示排序。
- `user_role` 是用户和角色的中间表，复合主键防止重复授权。
- `role_permission` 是角色和权限的中间表，复合主键防止重复分配。

### 4.4 好友申请（FriendRequest）与好友关系（Friendship）

好友申请和已建立的好友关系分开保存：

- `friend_request` 保存申请人、接收人、申请附言和处理状态
- `friendship` 只保存已经建立的对称好友关系
- 好友关系存储时统一按用户 ID 排序，同一对用户仅允许一条记录

对应 SQL 设计说明：

- `friend_request.request_id` 是好友申请的独立主键，供申请处理接口使用。
- `friend_request.message` 保存申请附言，`status` 表示待处理、已同意或已拒绝。
- 好友申请允许保留历史记录；业务层需保证同一对用户同时最多存在一条待处理申请。
- `friendship` 使用 `(user_id, friend_user_id)` 作为复合主键，不再额外引入自增 ID。
- `CHECK (user_id < friend_user_id)` 用来避免同一好友关系被存成两条方向相反的数据。
- 好友申请被同意后，由业务事务更新申请状态并创建 `friendship` 记录。

### 4.5 群聊（ChatGroup）与群成员（GroupMember）

群聊和成员是典型的一对多与多对多混合场景：

- 一个群聊拥有多个成员
- 一个用户可加入多个群聊
- `group_member` 使用复合主键 `(group_id, user_id)`
- 群管理员和群主通过 `member_role` 标识

对应 SQL 设计说明：

- `chat_group` 保存群本身的信息，包括 `group_name`、`avatar`、`owner_id`。
- `owner_id` 指向群主，是群级权限的最高拥有者。
- `group_member.member_role` 用数值区分群主、管理员、普通成员，便于权限判断。
- `group_member` 的复合主键确保一个用户在同一个群里只有一条成员记录。
- 邀请成员、踢出成员、本质上都是对 `group_member` 表的增删改。

### 4.6 消息（Message）

消息支持单聊和群聊，并承接文本、图片、Emoji、系统消息等场景。数据库层应保证：

- 单聊时 `receiver_id` 非空、`group_id` 为空
- 群聊时 `group_id` 非空、`receiver_id` 为空

对应 SQL 设计说明：

- `message_type` 区分文本、图片、系统消息、Emoji。
- `content` 统一存消息正文或资源路径，简化消息表结构。
- `sender_id` 在系统消息场景下可为空。
- `is_recalled` 用于支持消息撤回，而不是直接物理删除消息。
- `CHECK` 约束保证一条消息只能属于单聊或群聊其中一种场景。

### 4.7 在线文档（Document）相关表

文档模块由以下表组成：

- `document`：文档主体
- `document_permission`：用户协作权限
- `document_group_permission`：群组协作权限
- `document_favorite`：文档收藏

这对应 `spec.md` 中的文档创建、编辑、共享、收藏需求。

对应 SQL 设计说明：

- `document.title` 和 `document.content` 分别存标题和正文，正文可保存 Markdown 或富文本 JSON。
- `document.status` 区分私有、团队共享、归档状态。
- `document_permission` 用于控制单个协作者对文档的只读、评论、编辑、管理权限。
- `document_permission` 的复合主键防止同一用户对同一文档出现重复授权。
- `document_group_permission` 用于将文档授权给群组，群成员通过群组关系获得相应权限。
- `document_group_permission` 使用 `(document_id, group_id)` 复合主键，保证同一文档对同一群组只有一条授权。
- `document_favorite` 承接文档收藏需求，便于个人中心快速访问常用文档。

### 4.8 任务（Task）

根据需求，任务不仅有标题、负责人、截止时间，还包括：

- 开始时间
- 优先级
- 备注

因此，`task` 存储任务主体信息。

对应 SQL 设计说明：

- `task.creator_id` 表示任务创建人，`executor_id` 表示执行人。
- `priority` 用数值区分低、中、高优先级。
- `status` 用数值表示未开始、进行中、已完成、已取消。
- `start_time` 与 `deadline` 一起描述任务周期。
### 4.9 系统配置（SystemConfig）

`system_config` 使用 `config_key` / `config_value` 方式保存系统配置，适合开关项、默认值、展示文案等轻量配置。

---

## 5. 主要关系说明

```mermaid
erDiagram
    DEPARTMENT ||--o{ USER : contains
    USER ||--o{ USER_ROLE : has
    ROLE ||--o{ USER_ROLE : assigned_to
    ROLE ||--o{ ROLE_PERMISSION : owns
    PERMISSION ||--o{ ROLE_PERMISSION : granted_to

    USER ||--o{ FRIEND_REQUEST : requests
    USER ||--o{ FRIEND_REQUEST : receives
    USER ||--o{ FRIENDSHIP : user_side
    USER ||--o{ FRIENDSHIP : friend_side

    USER ||--o{ CHAT_GROUP : owns
    CHAT_GROUP ||--o{ GROUP_MEMBER : has
    USER ||--o{ GROUP_MEMBER : joins

    USER ||--o{ MESSAGE : sends
    USER o|--o{ MESSAGE : receives
    CHAT_GROUP o|--o{ MESSAGE : contains

    USER ||--o{ DOCUMENT : creates
    DOCUMENT ||--o{ DOCUMENT_PERMISSION : authorizes
    USER ||--o{ DOCUMENT_PERMISSION : receives
    DOCUMENT ||--o{ DOCUMENT_GROUP_PERMISSION : authorizes_group
    CHAT_GROUP ||--o{ DOCUMENT_GROUP_PERMISSION : receives
    USER ||--o{ DOCUMENT_FAVORITE : favorites
    DOCUMENT ||--o{ DOCUMENT_FAVORITE : favorited_by

    USER ||--o{ TASK : creates
    USER o|--o{ TASK : executes
```

---

## 6. 与 `spec.md` 的对齐说明

下列需求已明确映射到模型中：

1. **文档收藏**：新增 `document_favorite`
2. **系统配置**：新增 `system_config`
3. **任务开始时间**：`task.start_time`
4. **好友申请附言**：新增 `friend_request`
5. **文档群组权限**：新增 `document_group_permission`

以下需求不单独建表，而由现有表或业务逻辑支撑：

- 用户登录 JWT：由认证服务生成，不依赖专门业务表

---

## 7. 数据完整性约束

为保证数据一致性与可维护性，数据库应具备以下约束：

1. **主键约束**：每张表必须有主键；关联表使用复合主键。
2. **唯一约束**：用户名、邮箱、手机号、角色编码、权限编码、配置键必须唯一。
3. **外键约束**：核心引用关系使用外键保证数据有效性。
4. **非空约束**：用户名、密码、标题、创建人等关键字段不能为空。
5. **默认值约束**：创建时间、更新时间、状态等应提供合理默认值。
6. **检查约束**：消息目标、好友用户排序等应设置校验规则。
7. **索引优化**：对高频查询字段如用户名、状态、时间、负责人、所属部门建立索引。
8. **逻辑删除规范**：用户、部门、任务、文档等业务表可逻辑删除。

---

## 8. 规范总结

本次补全后的设计重点解决了以下问题：

1. 让模型和 `spec.md` 的功能范围保持一致。
2. 补齐了文档收藏、系统配置等缺失实体。
3. 保留了关联表不用自增主键的规范设计。
4. 让任务等字段更贴近实际需求。
5. 使核心实体和关联关系保持简洁、明确。

这套模型现在更适合作为课程设计文档、建表依据和后续接口实现基础。
