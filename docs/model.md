# SkyLink 团队协作办公平台数据模型设计

## 1 数据模型设计概述

SkyLink 是一款面向企业团队、校园组织及项目团队的轻量级协同办公平台，系统集成了用户管理、组织架构、即时通讯、任务协作、在线文档、文件管理、日程安排以及通知公告等多个业务模块。为了保证系统具有良好的可扩展性、数据一致性和维护性，数据库采用关系型数据库（MySQL）进行存储，并遵循第三范式（3NF）进行设计，在减少数据冗余的同时保证数据完整性。

数据库采用模块化设计思想，将系统划分为用户管理、组织管理、通讯管理、文件管理、任务管理、日程管理、文档管理以及系统管理等多个子模块，各模块之间通过主键、外键建立关联关系，实现业务数据共享与统一管理。

---

# 2 数据库总体结构

SkyLink 数据库主要由以下八大模块组成：

```
SkyLink Database
│
├── 用户管理模块
│   ├── 用户信息(User)
│   ├── 角色(Role)
│   ├── 权限(Permission)
│   └── 用户角色(UserRole)
│
├── 组织架构模块
│   ├── 部门(Department)
│   └── 部门成员(DepartmentMember)
│
├── 通讯模块
│   ├── 好友(Friend)
│   ├── 会话(Session)
│   ├── 群聊(Group)
│   ├── 群成员(GroupMember)
│   └── 消息(Message)
│
├── 文件管理模块
│   ├── 文件(File)
│   └── 文件共享(FileShare)
│
├── 在线文档模块
│   ├── 文档(Document)
│   └── 文档权限(DocumentPermission)
│
├── 任务管理模块
│   └── Task
│
├── 日程模块
│   └── Schedule
│
└── 系统管理模块
    ├── Notice
    ├── LoginLog
    └── OperationLog
```

整个系统共设计 **18 张核心数据表**。

---

# 3 实体及关系分析

## （1）用户（User）

用户是整个系统的核心实体。

一个用户可以：

* 属于一个部门
* 拥有多个角色
* 添加多个好友
* 创建多个群聊
* 加入多个群聊
* 创建多个任务
* 接收多个任务
* 创建多个文档
* 上传多个文件
* 创建多个日程

主要字段

| 字段            | 类型           | 说明   |
| ------------- | ------------ | ---- |
| user_id       | BIGINT       | 用户ID |
| username      | VARCHAR(50)  | 用户名  |
| password      | VARCHAR(255) | 密码   |
| nickname      | VARCHAR(50)  | 昵称   |
| avatar        | VARCHAR(255) | 头像   |
| email         | VARCHAR(100) | 邮箱   |
| phone         | VARCHAR(20)  | 手机号  |
| status        | TINYINT      | 状态   |
| department_id | BIGINT       | 所属部门 |
| create_time   | DATETIME     | 创建时间 |

---

## （2）部门（Department）

用于组织团队结构。

一个部门可以拥有多个成员。

| 字段              | 说明   |
| --------------- | ---- |
| department_id   | 部门编号 |
| department_name | 部门名称 |
| leader_id       | 负责人  |
| description     | 描述   |

关系：

```
Department
      │
      │1
      │
      └────────N User
```

---

## （3）角色（Role）

系统采用 RBAC 权限模型。

例如：

* 超级管理员
* 管理员
* 普通成员

字段

```
role_id
role_name
role_code
description
```

---

## （4）权限（Permission）

用于控制菜单及接口访问权限。

例如

```
user:add

user:update

task:create

document:edit
```

字段

```
permission_id
permission_name
permission_code
permission_type
```

---

## （5）用户角色关联（UserRole）

一个用户拥有多个角色。

一个角色可以对应多个用户。

属于典型多对多关系。

```
User
    │
    │N
    │
 UserRole
    │
    │N
    │
 Role
```

---

## （6）好友（Friend）

记录用户好友关系。

字段

```
friend_id

user_id

friend_user_id

status

create_time
```

---

## （7）群聊（Group）

一个群聊拥有多个成员。

字段

```
group_id

group_name

owner_id

avatar

notice

create_time
```

---

## （8）群成员（GroupMember）

建立群聊与成员关系。

```
Group
    │
    │1
    │
    └──────N GroupMember
                     │
                     │N
                     │
                   User
```

字段

```
group_id

user_id

role

join_time
```

---

## （9）消息（Message）

支持：

* 文本消息
* 图片消息
* 文件消息
* 系统消息

字段

| 字段           | 说明   |
| ------------ | ---- |
| message_id   | 消息ID |
| sender_id    | 发送者  |
| receiver_id  | 接收者  |
| group_id     | 群聊ID |
| message_type | 消息类型 |
| content      | 消息内容 |
| send_time    | 发送时间 |
| read_status  | 是否已读 |

---

## （10）文件（File）

记录上传文件信息。

字段

```
file_id

file_name

file_url

file_size

file_type

owner_id

upload_time
```

---

## （11）文件共享（FileShare）

控制文件共享权限。

字段

```
share_id

file_id

target_user

target_group

permission

expire_time
```

---

## （12）在线文档（Document）

支持多人在线编辑。

字段

```
document_id

title

content

creator_id

status

create_time

update_time
```

---

## （13）文档权限（DocumentPermission）

用于控制文档访问权限。

```
Document
      │
      │1
      │
      └──────N Permission
```

字段

```
document_id

user_id

permission_type
```

权限：

```
只读

评论

编辑

管理
```

---

## （14）任务（Task）

任务管理模块。

字段

| 字段          | 说明   |
| ----------- | ---- |
| task_id     | 任务编号 |
| title       | 任务标题 |
| content     | 任务描述 |
| creator_id  | 创建人  |
| executor_id | 负责人  |
| priority    | 优先级  |
| status      | 状态   |
| deadline    | 截止时间 |

---

## （15）日程（Schedule）

字段

```
schedule_id

title

content

user_id

start_time

end_time

remind_time

repeat_type
```

---

## （16）通知公告（Notice）

管理员发布通知。

字段

```
notice_id

title

content

publisher_id

publish_time

status
```

---

## （17）登录日志（LoginLog）

用于记录用户登录行为。

字段

```
log_id

user_id

login_ip

device

browser

login_time

login_result
```

---

## （18）操作日志（OperationLog）

记录系统重要操作。

字段

```
operation_id

user_id

module

operation

request_url

request_method

operation_time
```

---

# 4 数据实体关系（E-R）分析

系统主要实体之间的关系如下：

```
                 ┌────────────┐
                 │ Department │
                 └──────┬─────┘
                        │1
                        │
                        │N
                  ┌─────▼──────┐
                  │    User     │
                  └─┬────┬──────┘
                    │    │
          N         │    │N
     ┌────────┐     │    └────────────┐
     │UserRole│     │                 │
     └────┬───┘     │                 │
          │N        │                 │
          │         │                 │
          │1        │                 │
      ┌───▼───┐     │             ┌───▼────┐
      │ Role  │     │             │ Friend │
      └────────┘     │             └────────┘
                     │
                     │1
                     │
         ┌───────────┴────────────┐
         │                        │
   ┌─────▼─────┐            ┌─────▼─────┐
   │   Task    │            │ Schedule  │
   └───────────┘            └───────────┘
         │
         │
   ┌─────▼──────┐
   │ Document   │
   └─────┬──────┘
         │
         │
   ┌─────▼────────────┐
   │DocumentPermission│
   └──────────────────┘

User ───────────── Message ───────────── Group
           │                         │
           └────────GroupMember──────┘

User ───────────── File ───────────── FileShare

User ───────────── Notice

User ───────── LoginLog

User ─────── OperationLog
```

---

# 5 数据完整性约束

为保证数据一致性和业务正确性，系统设计如下约束：

1. **主键约束**：所有数据表均设置唯一主键，采用 `BIGINT` 自增或雪花算法生成唯一标识。
2. **外键约束**：用户、部门、角色、群组、任务等实体之间通过外键建立关联，确保引用数据有效。
3. **唯一性约束**：用户名、邮箱、手机号等字段设置唯一索引，避免重复注册。
4. **非空约束**：用户名、密码、任务标题、文档标题等关键字段不能为空。
5. **默认值约束**：状态字段、创建时间、更新时间等设置合理默认值，简化数据写入。
6. **逻辑删除**：业务数据采用逻辑删除方式保留历史记录，便于数据恢复与审计。
7. **索引优化**：对用户名、部门编号、消息时间、任务状态、文档标题等高频查询字段建立索引，提高检索效率。


