# RBAC 角色与权限管理：从数据库到接口拦截（小白注释版）

> 本文讲解 Sky Link 当前后端的 RBAC（Role-Based Access Control，基于角色的访问控制）实现。它解决的问题是：**一个已经登录的用户，具体可以做哪些事？**

## 1. 先用一句话理解 RBAC

不要直接给每个用户逐项勾选几百种能力，而是按下面的关系管理：

```text
用户 User
  └─ 通过 user_role 拥有一个或多个角色 Role
       └─ 通过 role_permission 拥有多个权限 Permission
            └─ 权限编码决定能否调用某个接口
```

例如：

```text
小明 → ROLE_ADMIN → "user:delete" → 可以调用删除用户接口
小红 → ROLE_USER  → 没有 "user:delete" → 调用时返回 403
```

JWT 负责确认“当前是谁”，RBAC 负责确认“这个人能做什么”。两者缺一不可。

---

## 2. 项目中的权限判定总流程

```text
请求 /api/v1/roles
  │
  ▼
JWT 拦截器验证 accessToken，得到 userId，放入 AuthContext
  │
  ▼
权限拦截器读取 Controller 上的 @RequirePermission("role:list")
  │
  ▼
按 userId 查询：user_role → role_permission → permission
  │
  ├─ 当前权限集合包含 role:list → 放行，执行 Controller
  └─ 不包含 → 抛 ForbiddenException，返回 403
```

## 3. 关键文件地图

| 用途 | 文件位置 |
| --- | --- |
| RBAC 数据表定义 | `backend/land/src/main/resources/schema.sql` |
| 权限要求注解 | `backend/land/src/main/java/com/skylink/land/auth/RequirePermission.java` |
| 每个请求的权限拦截器 | `backend/land/src/main/java/com/skylink/land/auth/PermissionAuthorizationInterceptor.java` |
| 用户角色、角色权限的查询 | `backend/land/src/main/java/com/skylink/land/service/identity/impl/UserServiceImpl.java` |
| 角色管理接口 | `backend/land/src/main/java/com/skylink/land/controller/RoleController.java` |
| 权限管理接口 | `backend/land/src/main/java/com/skylink/land/controller/PermissionController.java` |
| 给用户分配角色 | `backend/land/src/main/java/com/skylink/land/controller/UserController.java` |
| 启动时初始化内置角色/权限 | `backend/land/src/main/java/com/skylink/land/service/identity/bootstrap/SecurityDataInitializer.java` |
| 内置角色和权限清单 | `backend/land/src/main/java/com/skylink/land/service/identity/bootstrap/SecurityBootstrapCatalog.java` |

---

## 4. 数据库：RBAC 的四张核心表

**代码位置：** `backend/land/src/main/resources/schema.sql` 第 21–87 行。

```sql
-- 1. 用户表：谁在使用系统。
CREATE TABLE `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1, -- 0=禁用，1=启用
  PRIMARY KEY (`user_id`)
);

-- 2. 角色表：职位/身份的“权限包”。例如 ROLE_ADMIN、ROLE_USER。
CREATE TABLE `role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL, -- 给人看的名称，如“管理员”
  `role_code` VARCHAR(50) NOT NULL, -- 程序识别的唯一编码，如 ROLE_ADMIN
  `status` TINYINT NOT NULL DEFAULT 1, -- 0=禁用角色，1=启用角色
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
);

-- 3. 权限表：一个最小、可检查的操作能力。
CREATE TABLE `permission` (
  `permission_id` BIGINT NOT NULL AUTO_INCREMENT,
  `permission_name` VARCHAR(50) NOT NULL, -- 如“删除用户”
  `permission_code` VARCHAR(100) NOT NULL, -- 如 user:delete
  `permission_type` TINYINT NOT NULL DEFAULT 1, -- 1菜单、2按钮、3接口
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
);

-- 4. 用户与角色是多对多：一个人可有多个角色，一个角色可给多人。
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
);

-- 5. 角色与权限也是多对多：一个角色有多个权限，一个权限可被多个角色复用。
CREATE TABLE `role_permission` (
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);
```

`user_role` 和 `role_permission` 没有额外业务主键，而是使用两个 ID 的组合主键。这天然避免同一用户重复拥有同一角色、同一角色重复绑定同一权限。

## 5. 角色与权限编码怎么命名

项目把“资源:动作”作为权限编码，例如：

| 权限编码 | 含义 |
| --- | --- |
| `user:list` | 查看用户列表 |
| `user:role:add` | 给用户分配角色 |
| `role:permission:set` | 配置某个角色拥有的权限 |
| `document:update` | 更新文档元数据 |
| `message:send` | 发送聊天消息 |

**代码位置：** `SecurityBootstrapCatalog.java` 第 16–66 行。

```java
static final List<PermissionDefinition> PERMISSIONS = List.of(
    permission("List Users", "user:list", 13),
    permission("Assign User Roles", "user:role:add", 17),
    permission("Set Role Permissions", "role:permission:set", 44),
    permission("Create Document", "document:create", 60),
    permission("Send Message", "message:send", 25)
);
```

这里的中文/英文名称用于展示，真正给代码比较的是唯一的 `permissionCode`。

---

## 6. 如何给接口声明“需要什么权限”

### 6.1 `@RequirePermission` 是一个自定义注解

**代码位置：** `backend/land/src/main/java/com/skylink/land/auth/RequirePermission.java` 第 8–12 行。

```java
// @Target 表示此注解可写在“类”或“方法”上。
@Target({ElementType.METHOD, ElementType.TYPE})

// RUNTIME 表示程序运行时仍能通过反射读取它；拦截器正是这样工作的。
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    // 一个接口可以声明一个或多个权限编码。
    String[] value();
}
```

例如角色管理接口要求 `role:list`：

**代码位置：** `RoleController.java` 第 32–42 行。

```java
@GetMapping // 处理 GET /api/v1/roles
@RequirePermission("role:list") // 只有拥有 role:list 的用户才能进入方法
public PageResponse<AdminDto.RoleResponse> pageRoles(IdentityDto.RoleQueryRequest request) {
    // 能执行到这里，说明 JWT 身份和 RBAC 权限都已经验证通过。
    PageResponse<RoleVO> page = roleService.pageRoles(request);
    return PageResponse.<AdminDto.RoleResponse>builder()
        .total(page.getTotal())
        .page(page.getPage())
        .size(page.getSize())
        .records(page.getRecords().stream().map(this::toRoleResponse).toList())
        .build();
}
```

多个权限会使用“全部满足”的逻辑，因为拦截器调用了 `allMatch`。例如 `@RequirePermission({"a", "b"})` 表示必须同时拥有 `a` 和 `b`，不是任意一个即可。

---

## 7. 权限拦截器：每次请求怎样放行或拒绝

**代码位置：** `PermissionAuthorizationInterceptor.java` 第 25–60 行。

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 不是 Controller 方法的请求（例如静态资源）不参与此处的注解权限判断。
    if (!(handler instanceof HandlerMethod handlerMethod)) return true;

    // 先从方法上找 @RequirePermission；方法没写时再从 Controller 类上找。
    RequirePermission requirePermission = resolveRequiredPermission(handlerMethod);
    if (requirePermission == null || requirePermission.value().length == 0) {
        return true; // 没有声明权限要求，直接放行。
    }

    // JWT 拦截器已先执行；这里取到的是可信的当前登录用户 ID。
    Long userId = AuthContext.requireUserId();

    // 到数据库查当前用户现有的全部权限编码，再转为 Set 以便快速 contains 判断。
    Set<String> userPermissions = userService.listPermissionCodes(userId).stream()
        .collect(Collectors.toSet());
    if (CollectionUtils.isEmpty(userPermissions)) {
        throw new ForbiddenException("permission denied"); // 已登录但什么权限也没有：403。
    }

    // 接口声明的每一项权限都必须在用户权限集合中。
    boolean allowed = Arrays.stream(requirePermission.value())
        .allMatch(userPermissions::contains);
    if (!allowed) throw new ForbiddenException("permission denied");

    return true; // 只有这里返回 true，Spring 才会执行 Controller 方法。
}
```

请求顺序是：**JWT 认证拦截器 → RBAC 权限拦截器 → Controller**。配置位置见 `WebMvcConfiguration.java` 第 50–58 行。

## 8. 后端如何从用户查到权限

**代码位置：** `UserServiceImpl.java` 第 261–281 行。

```java
public List<String> listPermissionCodes(Long userId) {
    // 第一步：user_role → role，取出用户拥有且状态为启用的角色 ID。
    List<Long> roleIds = listEnabledRoles(userId).stream()
        .map(Role::getRoleId)
        .toList();
    if (CollectionUtils.isEmpty(roleIds)) return List.of();

    // 第二步：role_permission，取出这些角色绑定的所有权限 ID。
    List<Long> permissionIds = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
        ).stream()
        .map(RolePermission::getPermissionId)
        .distinct() // 用户有多个角色时，同一权限只保留一次。
        .toList();
    if (CollectionUtils.isEmpty(permissionIds)) return List.of();

    // 第三步：permission，取出最终供 @RequirePermission 比较的字符串编码。
    return permissionMapper.selectBatchIds(permissionIds).stream()
        .map(Permission::getPermissionCode)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
}
```

这段逻辑说明一个很重要的事实：**本项目不是只相信 JWT 里登录时写入的 roles，而是在每次受注解保护的请求中重新查询数据库权限。**

因此管理员修改角色权限后，不必等旧 JWT 过期；下一次接口请求会按数据库中的最新角色—权限关系判断。

---

## 9. 管理员怎样给用户分配角色

**接口位置：** `POST /api/v1/users/{userId}/roles`，代码在 `UserController.java` 第 100–109 行。

```java
@PostMapping("/{userId}/roles")
@RequirePermission("user:role:add") // 不是任何登录用户都能改别人的角色。
public List<UserDto.UserRoleInfo> assignUserRoles(
    @PathVariable Long userId,
    @RequestBody UserDto.AssignUserRolesRequest request
) {
    if (request == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
    }
    return toRoleInfos(userService.assignRoles(userId, request.getRoleIds()));
}
```

真正替换用户角色的逻辑：

**代码位置：** `UserServiceImpl.java` 第 285–298 行。

```java
@Transactional(rollbackFor = Exception.class)
public List<RoleVO> assignRoles(Long userId, List<Long> roleIds) {
    requireUser(userId); // 用户不存在时不允许写关联数据。

    // 这是“覆盖式分配”：先清空该用户所有旧角色，再写入新的角色集合。
    userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

    // 去重、校验 roleId；若列表为空，用户最终没有任何角色。
    List<Long> normalizedRoleIds = normalizeIds("roleIds", roleIds);
    if (CollectionUtils.isEmpty(normalizedRoleIds)) return List.of();

    // 防止把不存在的角色 ID 写入 user_role。
    ensureRolesExist(normalizedRoleIds);
    normalizedRoleIds.stream()
        .map(roleId -> buildUserRole(userId, roleId))
        .forEach(userRoleMapper::insert);
    return listRoles(userId);
}
```

注册用户或创建用户但没有指定角色时，项目会自动绑定默认 `ROLE_USER`。相关逻辑见 `AuthServiceImpl.java` 第 199–210 行与 `UserServiceImpl.java` 第 121–129 行。

## 10. 管理员怎样为角色配置权限

**接口位置：** `PUT /api/v1/roles/{roleId}/permissions`，代码在 `RoleController.java` 第 65–75 行。

```java
@PutMapping("/{roleId}/permissions")
@RequirePermission("role:permission:set")
public AdminDto.RoleResponse assignRolePermissions(
    @PathVariable Long roleId,
    @RequestBody AdminDto.AssignRolePermissionsRequest request
) {
    if (request == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");

    // 前端传入 permissionCodes，例如 ["message:list", "message:send"]。
    return toRoleResponse(roleService.assignPermissionCodes(roleId, request.getPermissionCodes()));
}
```

角色权限也是“替换式”保存：

**代码位置：** `RoleServiceImpl.java` 第 159–171 行。

```java
@Transactional(rollbackFor = Exception.class)
public void assignPermissions(Long roleId, List<Long> permissionIds) {
    requireRole(roleId); // 先确认角色存在。

    // 先删除旧的 role_permission 关联，后写入新的完整集合。
    rolePermissionMapper.delete(
        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
    );

    List<Long> normalizedPermissionIds = normalizeIds("permissionIds", permissionIds);
    if (CollectionUtils.isEmpty(normalizedPermissionIds)) return;

    ensurePermissionsExist(normalizedPermissionIds); // 不允许绑定不存在的权限。
    normalizedPermissionIds.stream()
        .map(permissionId -> buildRolePermission(roleId, permissionId))
        .forEach(rolePermissionMapper::insert);
}
```

## 11. 系统默认有哪些角色

**代码位置：** `SecurityBootstrapCatalog.java` 第 8–14、69–131 行。

| 角色编码 | 定位 |
| --- | --- |
| `ROLE_USER` | 已认证普通用户的基础权限，如聊天、查看/编辑自身可访问的文档、查看任务。 |
| `ROLE_PROJECT_LEADER` | 在基础用户权限上增加任务创建、更新等项目任务管理能力。 |
| `ROLE_ADMIN` | 在基础权限上增加用户、部门、角色、权限的管理能力。 |
| `ROLE_SUPER_ADMIN` | 启动时绑定数据库当前全部权限。 |

初始化器会确保这些角色和权限存在，并把权限关系补齐：

**代码位置：** `SecurityDataInitializer.java` 第 57–85 行。

```java
public void run(ApplicationArguments args) {
    // 1. 若内置权限不存在则创建；若被逻辑删除则恢复。
    Map<String, Permission> permissions = ensurePermissions();

    // 2. 确保四个内置角色存在。
    Role userRole = ensureRole("User", ROLE_USER, "Default role for authenticated users");
    Role adminRole = ensureRole("Administrator", ROLE_ADMIN, "Manages users, departments, and roles");
    Role projectLeaderRole = ensureRole("Project Leader", ROLE_PROJECT_LEADER, "Manages project tasks");
    Role superAdminRole = ensureRole("Super Administrator", ROLE_SUPER_ADMIN, "Has all system permissions");

    // 3. 把预定义权限绑定给前三种角色。
    bindPermissions(userRole, permissions, USER_PERMISSION_CODES);
    bindPermissions(adminRole, permissions, ADMIN_PERMISSION_CODES);
    bindPermissions(projectLeaderRole, permissions, PROJECT_LEADER_PERMISSION_CODES);

    // 4. 给没有角色的启用用户补上默认 ROLE_USER。
    bindDefaultRoleToUsersMissingRoles(userRole);

    // 5. 超级管理员获得数据库内全部权限，而非一份固定清单。
    bindPermissions(superAdminRole, allPermissions, allPermissions.keySet().stream().toList());
}
```

初始化开关在 `application.yaml`：`skylink.bootstrap.security-data-enabled`。默认值是 `true`。

---

## 12. RBAC 和“资源自身权限”不是一回事

RBAC 只能回答“用户是否可调用文档更新这个功能”；它不能单独回答“用户能否编辑第 123 号文档”。

因此在线文档还有第二层资源级权限：

```text
@RequirePermission("document:update")  → RBAC：是否可使用“更新文档”功能
DocumentService.resolvePermission(...) → 资源权限：是否有这篇文档的 edit/manage 权限
```

这两层必须都通过。文档详情见 [在线文档讲解](online-document-guide.md)。

## 13. 小白最容易混淆的点

| 容易混淆 | 正确理解 |
| --- | --- |
| 角色就是权限 | 不是。角色是权限的集合；真正被接口检查的是权限编码。 |
| 一个用户只能有一个角色 | 不是。`user_role` 允许多角色，最终权限会去重合并。 |
| 登录成功就能调所有接口 | 不是。登录只证明身份，RBAC 仍可能拒绝接口并返回 403。 |
| 前端隐藏按钮就足够安全 | 不够。前端仅改善体验；后端 `@RequirePermission` 才是安全边界。 |
| 修改角色权限后必须让用户重新登录 | 当前实现通常不需要，因为拦截器每次请求会查询数据库权限。 |
| `ROLE_ADMIN` 天生在代码里拥有一切 | 不完全是。它拥有初始化器定义的权限集合；`ROLE_SUPER_ADMIN` 才在初始化时绑定全部权限。 |

## 14. 最后用人话复盘

1. 用户登录后，JWT 提供可信的 `userId`。
2. 受保护 Controller 写上 `@RequirePermission("资源:动作")`。
3. 拦截器用 `userId` 查用户角色，再查角色权限，得到一组权限编码。
4. 编码匹配则放行，不匹配则 403。
5. 管理员可通过“用户—角色”和“角色—权限”两套接口调整授权关系。
6. 对文档等具体业务资源，还会在 RBAC 放行后继续做文档自身的读/编辑/管理权限检查。

