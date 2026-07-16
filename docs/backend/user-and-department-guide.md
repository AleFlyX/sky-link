# 用户与部门管理：账号、归属与成员迁移（小白注释版）

> 本文只说明 Sky Link 当前已经实现的用户与部门管理代码，不修改业务逻辑。它把“用户账号”“用户角色”和“用户所属部门”分开管理：角色负责系统权限，部门负责组织归属。

## 1. 先用一句话理解

用户与部门的关系是：**一个用户最多属于一个部门，一个部门可以有多个用户。**

```text
用户 User
  ├─ department_id → 所属部门 Department（可为空）
  └─ user_role → 系统角色 Role（可多个，用于 RBAC）

部门 Department
  ├─ leader_id → 负责人用户（可为空）
  └─ 通过所有 user.department_id = 本部门 ID 找到成员
```

部门成员不是单独的“成员关联表”，而是直接修改用户表中的 `department_id`。把用户加入另一个部门，本质上就是把这个字段改成新的部门 ID。

## 2. 完整流程图

```text
管理员在前端创建用户
  → POST /users
  → 校验账号、邮箱、手机号、密码、部门、角色
  → 写入 user + 写入 user_role

管理员创建部门
  → POST /departments
  → 校验部门名称唯一、负责人用户存在
  → 写入 department

管理员给部门添加成员
  → POST /departments/{departmentId}/members
  → 批量更新每个 user.department_id
  → 用户自动从原部门迁移到新部门

删除部门
  → 先统计仍归属该部门的用户
  ├─ 有成员：拒绝删除
  └─ 无成员：删除部门
```

## 3. 关键文件地图

| 用途 | 文件位置 |
| --- | --- |
| 用户接口 | `backend/land/src/main/java/com/skylink/land/controller/UserController.java` |
| 部门接口 | `backend/land/src/main/java/com/skylink/land/controller/DepartmentController.java` |
| 用户创建、查询、启停、角色分配 | `backend/land/src/main/java/com/skylink/land/service/identity/impl/UserServiceImpl.java` |
| 部门、负责人、成员迁移 | `backend/land/src/main/java/com/skylink/land/service/identity/impl/DepartmentServiceImpl.java` |
| 用户/部门数据库表 | `backend/land/src/main/resources/schema.sql` |
| 用户管理页 | `frontend/sky-link-frontend/src/views/users/UserListView.vue` |
| 部门管理状态 | `frontend/sky-link-frontend/src/views/departments/composables/useDepartmentManagement.js` |
| 部门前端 API | `frontend/sky-link-frontend/src/api/department.js` |

---

## 4. 数据库关系：为什么用户只有一个部门

**代码位置：** `backend/land/src/main/resources/schema.sql` 第 21–40、4–15 行。

```sql
CREATE TABLE `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT, -- 用户唯一 ID
  `username` VARCHAR(50) NOT NULL,          -- 登录用户名，唯一
  `password` VARCHAR(255) NOT NULL,         -- 保存密码哈希，不能保存明文
  `status` TINYINT NOT NULL DEFAULT 1,      -- 0=禁用，1=启用
  `department_id` BIGINT DEFAULT NULL,      -- 当前所属部门；NULL 表示未分配部门
  PRIMARY KEY (`user_id`),
  KEY `idx_user_department_id` (`department_id`)
);

CREATE TABLE `department` (
  `department_id` BIGINT NOT NULL AUTO_INCREMENT, -- 部门唯一 ID
  `department_name` VARCHAR(50) NOT NULL,         -- 部门名称，唯一
  `leader_id` BIGINT DEFAULT NULL,                 -- 负责人用户 ID，可不设置
  `description` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `uk_department_name` (`department_name`)
);
```

`user.department_id` 是一个单值字段，所以同一用户不能同时属于多个部门。若要改变归属，直接更新该字段；旧部门不需要额外删除成员记录。

## 5. 系统权限：谁能操作用户和部门

用户与部门管理属于 RBAC 管理范围。后端接口通过 `@RequirePermission` 做最终权限判断，前端的 `v-permission` 只负责隐藏无权限按钮。

| 操作 | 权限编码 |
| --- | --- |
| 查看用户列表/详情 | `user:get` |
| 创建用户 | `user:create` |
| 启用、禁用用户 | `user:status:update` |
| 删除用户 | `user:delete` |
| 分配角色 | `user:role:add` |
| 查看部门和成员 | `department:list`、`department:members:list` |
| 创建、编辑、删除部门 | `department:create`、`department:update`、`department:delete` |
| 添加、移出部门成员 | `department:members:add`、`department:members:remove` |

**代码位置：** `UserController.java` 第 64–115 行。

```java
@GetMapping
@RequirePermission("user:get") // 当前项目的用户列表接口要求 user:get。
public PageResponse<UserSummaryResponse> pageUsers(UserQueryRequest request) {
    // Service 按用户名、昵称、部门、状态查询分页用户。
    PageResponse<UserVO> page = userService.pageUsers(request);
    return PageResponse.<UserSummaryResponse>builder()
        .total(page.getTotal())
        .page(page.getPage())
        .size(page.getSize())
        .records(page.getRecords().stream().map(this::toUserSummaryResponse).toList())
        .build();
}

@PostMapping
@RequirePermission("user:create") // 只有具备创建用户权限的人能新增账号。
public UserSummaryResponse createUser(@RequestBody CreateUserRequest request) {
    return toUserSummaryResponse(userService.createUser(request));
}

@PutMapping("/{userId}/status")
@RequirePermission("user:status:update")
public UserSummaryResponse updateUserStatus(...) {
    // 管理员可以将账号状态从启用改为禁用，或反向恢复。
    return toUserSummaryResponse(userService.updateUserStatus(userId, request.getStatus()));
}
```

完整的 RBAC 查询和拦截过程可阅读 [RBAC 角色与权限管理讲解](rbac-role-permission-guide.md)。

---

## 6. 创建用户：账号、部门和角色如何一起保存

**代码位置：** `UserServiceImpl.java` 第 80–132 行。

```java
@Transactional(rollbackFor = Exception.class) // 用户表或角色关联任一步失败，整体回滚。
public UserVO createUser(CreateUserRequest request) {
    // 1. 必填项校验：不能创建没有账号、密码、邮箱或手机号的用户。
    if (request == null || !StringUtils.hasText(request.getUsername())
        || !StringUtils.hasText(request.getPassword())
        || !StringUtils.hasText(request.getEmail())
        || !StringUtils.hasText(request.getPhone())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "username, password, email and phone are required");
    }

    // 2. 密码必须至少 8 位，且同时含有字母和数字。
    if (!isValidPassword(request.getPassword())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "password must be at least 8 characters and contain letters and numbers");
    }

    // 3. 去除首尾空格，并保证用户名、邮箱、手机号在系统中都不重复。
    String username = request.getUsername().trim();
    String email = request.getEmail().trim();
    String phone = request.getPhone().trim();
    ensureUniqueUsername(username, null);
    ensureUniqueEmail(email, null);
    ensureUniquePhone(phone, null);

    // 4. 指定部门时，必须先确认该部门真实存在。
    if (request.getDepartmentId() != null) {
        ensureDepartmentExists(request.getDepartmentId());
    }

    // 5. 密码先用 PasswordEncoder 哈希，再写入 user 表；绝不写入明文密码。
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
    user.setEmail(email);
    user.setPhone(phone);
    user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    user.setDepartmentId(request.getDepartmentId());
    save(user);

    // 6. 有 roleIds 时写入 user_role；没有指定角色时，自动绑定默认 ROLE_USER。
    List<Long> roleIds = normalizeIds("roleIds", request.getRoleIds());
    if (CollectionUtils.isEmpty(roleIds)) {
        bindDefaultRole(user.getUserId());
    } else {
        ensureRolesExist(roleIds);
        roleIds.stream().map(roleId -> buildUserRole(user.getUserId(), roleId))
            .forEach(userRoleMapper::insert);
    }

    return getUserVO(user.getUserId());
}
```

用户的部门归属与角色不是一回事：`departmentId` 表示组织位置，`roleIds` 表示系统允许做什么。

## 7. 前端用户管理页怎样筛选和创建

**代码位置：** `frontend/sky-link-frontend/src/views/users/UserListView.vue` 第 146–167、197–221 行。

```js
async function loadUsers(targetPage = page.value) {
  const response = await getUsers({
    page: targetPage,
    size: pageSize,
    username: filters.username.trim() || undefined, // 未填写时不传筛选条件。
    nickname: filters.nickname.trim() || undefined,
    departmentId: normalizeId(filters.departmentId), // 部门下拉框筛选。
    status: normalizeId(filters.status),             // 启用/禁用状态筛选。
  })

  const data = unwrapData(response)
  rows.value = data.records || []
  total.value = data.total || 0
}

async function saveUser(payload) {
  // 前端将表单的字符串 ID 转为数字，空部门转为 undefined。
  await createUserApi({
    username: payload.username.trim(),
    password: payload.password,
    nickname: payload.nickname.trim() || undefined,
    email: payload.email.trim(),
    phone: payload.phone.trim(),
    departmentId: normalizeId(payload.departmentId),
    status: normalizeId(payload.status) ?? 1,
    roleIds: Array.isArray(payload.roleIds)
      ? payload.roleIds.map(Number).filter(Number.isFinite)
      : [],
  })
  await loadUsers(page.value) // 创建成功后重新拉取当前页，保证数据来自后端。
}
```

前端按钮会使用 `v-permission="'user:create'"` 隐藏，但后端仍会再次检查 `@RequirePermission("user:create")`。

---

## 8. 创建和修改部门

**代码位置：** `DepartmentServiceImpl.java` 第 65–110 行。

```java
@Transactional(rollbackFor = Exception.class)
public DepartmentVO createDepartment(SaveDepartmentRequest request) {
    // 1. 部门名称必填。
    if (request == null || !StringUtils.hasText(request.getDepartmentName())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "departmentName is required");
    }

    // 2. 如果设置负责人，负责人用户必须存在。
    //    当前实现只检查“用户存在”，并不要求负责人必须属于本部门。
    validateLeaderId(request.getLeaderId());

    // 3. 部门名称不可重复。
    ensureDepartmentNameUnique(request.getDepartmentName().trim(), null);

    // 4. 写入部门基础信息；此步骤不会自动添加任何成员。
    Department department = new Department();
    department.setDepartmentName(request.getDepartmentName().trim());
    department.setLeaderId(request.getLeaderId());
    department.setDescription(normalize(request.getDescription()));
    save(department);

    return getDepartmentVO(department.getDepartmentId());
}
```

部门负责人和部门成员是两个不同概念。负责人存于 `department.leader_id`；成员由 `user.department_id` 决定。当前代码允许先设置负责人，再单独添加成员。

## 9. 添加成员：实际上是在迁移用户

**代码位置：** `DepartmentServiceImpl.java` 第 151–178 行。

```java
@Transactional(rollbackFor = Exception.class)
public PageResponse<UserVO> addDepartmentMembers(Long departmentId, List<Long> userIds) {
    // 1. 目标部门必须存在。
    Department department = getById(departmentId);
    if (department == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
    }

    // 2. 去重并拒绝空 ID；例如 [3, 3, 5] 会规范成 {3, 5}。
    Set<Long> normalizedUserIds = normalizeUserIds(userIds);

    // 3. 所有用户都必须存在，避免“只成功迁移一部分不存在的用户”。
    List<User> users = userMapper.selectBatchIds(normalizedUserIds);
    if (users.size() != normalizedUserIds.size()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "some users do not exist");
    }

    // 4. 对每个用户更新唯一的 department_id。
    //    若用户原先在别的部门，这一写入会直接覆盖旧部门 ID，也就是自动迁移。
    for (User user : users) {
        userMapper.update(null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, user.getUserId())
                .set(User::getDepartmentId, departmentId));
    }

    // 5. 返回当前部门的成员分页数据给调用方。
    return pageDepartmentMembers(departmentId, request);
}
```

前端在打开“添加成员”时会排除已经属于当前部门的用户，但会保留其他部门用户，因此管理员可以把他们迁移过来：

**代码位置：** `useDepartmentManagement.js` 第 103–117 行。

```js
const availableMemberOptions = computed(() => {
  return leaderOptions.value.filter((user) => {
    // 用户已在当前部门时排除，防止重复选择。
    if (user.departmentId != null && activeDepartment.value.departmentId != null) {
      return Number(user.departmentId) !== Number(activeDepartment.value.departmentId)
    }

    // 其他部门或未分配部门的用户仍可被选择，后端会把他们迁移到当前部门。
    return user.departmentName !== activeDepartment.value.departmentName
  })
})
```

## 10. 移出成员和删除部门的保护

### 10.1 移出成员只会清空部门归属

**代码位置：** `DepartmentServiceImpl.java` 第 180–205 行。

```java
public void removeDepartmentMember(Long departmentId, Long userId) {
    // 确认部门、用户都存在，并确认此用户现在确实属于该部门。
    if (!departmentId.equals(user.getDepartmentId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "user is not in this department");
    }

    // 设置为 null：用户变成“未分配部门”，不会删除用户账号。
    userMapper.update(null,
        new LambdaUpdateWrapper<User>()
            .eq(User::getUserId, userId)
            .set(User::getDepartmentId, null));
}
```

### 10.2 非空部门不能删除

**代码位置：** `DepartmentServiceImpl.java` 第 113–129 行。

```java
public void deleteDepartment(Long departmentId) {
    Department department = getById(departmentId);
    if (department == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
    }

    // 删除前统计仍归属此部门的用户数量。
    Long memberCount = userMapper.selectCount(
        new LambdaQueryWrapper<User>().eq(User::getDepartmentId, departmentId)
    );
    if (memberCount != null && memberCount > 0) {
        // 防止用户留下一个不存在的 department_id。
        throw new BusinessException(ErrorCode.CONFLICT, "department still has members");
    }

    removeById(departmentId); // 只有空部门可以删除。
}
```

这也是部门页面提示“非空部门需先迁移成员后再删除”的后端依据。

## 11. 常见误解

| 容易混淆 | 正确理解 |
| --- | --- |
| 用户角色和所属部门是一回事 | 不是。角色决定系统权限；部门表示组织归属。 |
| 给部门添加成员会创建成员关联表 | 不会。当前实现直接更新 `user.department_id`。 |
| 一个用户可以同时在多个部门 | 不可以。`department_id` 是单一字段。 |
| 移出部门会删除用户 | 不会。只把 `department_id` 设为 `null`。 |
| 删除部门会自动删除成员 | 不会。部门有成员时后端直接拒绝删除。 |
| 部门负责人一定是该部门成员 | 当前实现只验证负责人用户存在，不强制其属于该部门。 |
| 前端隐藏“删除部门”按钮就安全 | 不够。后端 `@RequirePermission("department:delete")` 才是最终权限边界。 |

## 12. 最后用人话复盘

1. 用户账号保存身份资料、状态和唯一的部门 ID；用户角色另存在 `user_role` 中。
2. 创建用户时会校验唯一性和密码，再同时处理部门归属与角色绑定。
3. 创建部门只保存部门基础信息和可选负责人，不会自动添加成员。
4. 添加成员会批量更新用户的 `department_id`，因此天然实现跨部门迁移。
5. 移出成员只清空归属；删除部门前必须先确保没有成员。
6. 所有管理接口都由后端 RBAC 检查，前端权限指令只辅助界面展示。

