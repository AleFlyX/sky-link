# 三天分工文档 - 成员 A（后端主线一）

## 负责范围

成员 A 负责系统基础能力与权限中台，包括认证、用户、部门、角色、权限、统一响应和日志基础能力。

对应模块：

- 认证：`/auth/**`
- 用户：`/users/**`
- 部门：`/departments/**`
- 角色权限：`/roles/**`、`/permissions/**`、`/users/{userId}/roles/**`
- 日志基础：`/logs/login`、`/logs/operation`

对应重点表：

- `user` `department`
- `role` `permission`
- `user_role` `role_permission`
- `login_log` `operation_log`

## 落地目标

- 第 1 天结束前，其他两人能拿到统一的鉴权方式、用户结构和分页结构。
- 第 2 天结束前，用户、部门、角色权限主流程接口可联调。
- 第 3 天结束前，A 线接口达到“能支撑登录和管理页演示”的状态。

## 任务拆解

### 必做

- 统一返回体 `Result<T>`
- 全局异常处理
- JWT 登录鉴权
- 当前用户解析
- 用户 CRUD
- 部门 CRUD
- 角色/权限 CRUD
- 用户分配角色
- 登录日志、操作日志查询

### 可延后

- refresh token
- 更细粒度权限拦截
- 更完整的日志筛选条件

## Day 1

- 完成 Spring Boot 基础项目结构补全：统一返回体、异常处理、分页结构、JWT 认证拦截设计。
- 完成 `user`、`department`、`role`、`permission`、关联表的实体、DTO、VO、Mapper、Service 框架。
- 输出接口字段与状态码约定，发给另外两人复用。

具体清单：

- 建立认证相关包结构：`controller`、`service`、`dto`、`vo`
- 完成 `/auth/register`、`/auth/login` 的请求和返回结构定义
- 完成 `/users/me`、`/users`、`/departments`、`/roles`、`/permissions` 接口骨架
- 在群里同步一份接口约定：
  - Token 放在 `Authorization: Bearer xxx`
  - 分页返回结构
  - 用户信息字段名

交付物：

- 可编译的基础后端框架
- 用户/部门/角色权限相关实体与接口骨架
- 一份可供成员 C 调前端的接口字段示例

## Day 2

- 实现注册、登录、退出、获取当前用户、更新个人信息、修改密码。
- 实现部门 CRUD、部门成员列表、用户列表、用户状态更新、用户逻辑删除。
- 实现角色列表、创建角色、分配权限、用户分配角色。

具体清单：

- 跑通 `/auth/register` `/auth/login` `/auth/logout`
- 跑通 `/users/me` `/users/me/password`
- 跑通 `/users` `/users/{userId}` `/users/{userId}/status`
- 跑通 `/departments` 增删改查和 `/departments/{id}/members`
- 跑通 `/roles` `/roles/{roleId}/permissions` `/users/{userId}/roles`

交付物：

- A 线核心接口可本地联调
- Postman 或接口自测记录
- 至少 8 个接口有成功示例返回

## Day 3

- 补齐登录日志、操作日志查询接口。
- 联调全栈同学的登录页、个人中心、用户/部门管理页。
- 修正权限校验、统一错误码、补充接口文档说明。

具体清单：

- 跑通 `/logs/login`、`/logs/operation`
- 配合成员 C 修复登录失败、Token 失效、分页展示等前端问题
- 配合成员 B 提供用户身份、角色校验、管理员权限判断支持
- 整理“已完成接口清单”和“未完成但可兜底部分”

验收标准：

- 登录注册完整可跑通
- 管理员可管理用户、部门、角色
- 统一响应格式和鉴权规则稳定可复用
- 成员 C 能不改接口协议直接接入前端页面

## 提交节奏

- Day 1 中午前提交基础框架
- Day 1 晚上提交认证和用户骨架
- Day 2 晚上提交 A 线主接口
- Day 3 下午只做 bugfix 和联调，不再大改结构

## 风险与兜底

- 如果 JWT 来不及做完整刷新机制，先保证登录、鉴权、退出可用。
- 如果权限细粒度控制来不及，先做管理员/普通用户两级判断。
- 如果日志筛选不完善，先保证日志查询列表可返回数据。

## 协作说明

- 需要最早完成并同步：JWT 规则、用户信息结构、分页格式。
- 对成员 B 提供统一用户身份、角色权限校验支持。
- 对成员 C 提供登录态、当前用户信息和管理端接口联调支持。
