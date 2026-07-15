# 角色权限关系

来源：`backend/land/src/main/resources/data.sql`

## 角色

- `ROLE_SUPER_ADMIN` - Super Administrator
- `ROLE_ADMIN` - Administrator
- `ROLE_PROJECT_LEADER` - Project Leader
- `ROLE_USER` - User

## 权限总表

- `user:me:get`
- `user:me:update`
- `user:password:update`
- `user:list`
- `user:get`
- `user:status:update`
- `user:delete`
- `user:role:add`
- `user:role:delete`
- `friend:list`
- `friend:request:list`
- `friend:request:create`
- `friend:delete`
- `message:list`
- `message:send`
- `message:recall`
- `group:list`
- `department:list`
- `department:create`
- `department:update`
- `department:delete`
- `department:members:list`
- `role:list`
- `role:create`
- `role:update`
- `role:delete`
- `role:permission:set`
- `permission:list`
- `permission:create`
- `permission:update`
- `permission:delete`
- `document:create`
- `document:list`
- `document:get`
- `document:update`
- `document:delete`
- `document:permission:user:set`
- `document:permission:group:set`
- `document:permission:list`
- `document:permission:user:delete`
- `document:permission:group:delete`
- `task:create`
- `task:list`
- `task:get`
- `task:update`
- `task:status:update`
- `task:delete`

## 角色权限映射

### `ROLE_SUPER_ADMIN`

拥有全部 47 个权限。

### `ROLE_ADMIN`

- 基础：`user:me:get` `user:me:update` `user:password:update` `user:get` `friend:list` `friend:request:list` `friend:request:create` `friend:delete` `message:list` `message:send` `message:recall` `group:list`
- 用户：`user:list` `user:get` `user:status:update` `user:delete` `user:role:add` `user:role:delete`
- 部门：`department:list` `department:create` `department:update` `department:delete` `department:members:list`
- 角色：`role:list` `role:create` `role:update` `role:delete` `role:permission:set`
- 权限：`permission:list` `permission:create` `permission:update` `permission:delete`
- 文档：`document:create` `document:list` `document:get` `document:update` `document:delete` `document:permission:user:set` `document:permission:group:set` `document:permission:list` `document:permission:user:delete` `document:permission:group:delete`
- 任务：`task:create` `task:list` `task:get` `task:update` `task:status:update` `task:delete`

### `ROLE_PROJECT_LEADER`

- 个人：`user:me:get` `user:me:update` `user:password:update`
- 用户：`user:get`
- 好友/消息/群组：`friend:list` `friend:request:list` `friend:request:create` `friend:delete` `message:list` `message:send` `message:recall` `group:list`
- 部门：`department:list` `department:members:list`
- 文档：`document:create` `document:list` `document:get` `document:update` `document:delete` `document:permission:user:set` `document:permission:group:set` `document:permission:list` `document:permission:user:delete` `document:permission:group:delete`
- 任务：`task:list` `task:get` `task:status:update` `task:create` `task:update`

### `ROLE_USER`

- 个人：`user:me:get` `user:me:update` `user:password:update`
- 用户：`user:get`
- 好友/消息/群组：`friend:list` `friend:request:list` `friend:request:create` `friend:delete` `message:list` `message:send` `message:recall` `group:list`
- 部门：`department:list` `department:members:list`
- 文档：`document:create` `document:list` `document:get` `document:update` `document:delete` `document:permission:user:set` `document:permission:group:set` `document:permission:list` `document:permission:user:delete` `document:permission:group:delete`
- 任务：`task:list` `task:get` `task:status:update`

## 备注

- `ROLE_PROJECT_LEADER` 现在是基础用户权限加任务管理权限。
- `ROLE_SUPER_ADMIN` 由 `data.sql` 通过 `CROSS JOIN permission` 自动绑定全部权限。
- 登录响应应返回 `permissions`，前端可直接据此隐藏侧边栏入口。
- 前端路由守卫也应读取 `meta.permissions`，无权限时跳转 `/401`。
- 当前 `permission` 表里全部都是接口权限，没有单独的菜单权限码。
