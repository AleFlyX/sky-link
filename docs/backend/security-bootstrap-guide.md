# 启动安全数据初始化：角色、权限和初始管理员从哪来（小白版）

项目启动时会检查安全基础数据是否齐全：权限目录、四个系统角色、角色-权限关系、没有角色的已启用用户，以及可选的初始超级管理员。它不是“系统配置”业务模块，也不讲尚未实现的 `system_config`；本篇只解释已经实现的安全数据引导。

配合阅读：[RBAC 角色与权限教程](rbac-role-permission-guide.md)。

## 完整流程

```text
Spring Boot 启动完成
  -> SecurityDataInitializer.run()
  -> 确保权限目录存在（被逻辑删除则恢复）
  -> 确保 ROLE_USER / ROLE_ADMIN / ROLE_PROJECT_LEADER / ROLE_SUPER_ADMIN 存在
  -> 补齐各角色缺少的权限关联
  -> 给“已启用但没有任何角色”的用户补 ROLE_USER
  -> 给超级管理员角色补全部权限
  -> 若启用管理员引导：创建或校验管理员，并绑定超级管理员角色
  -> 任一步失败，整个事务回滚
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| 启动入口与补齐逻辑 | `backend/land/src/main/java/com/skylink/land/service/identity/bootstrap/SecurityDataInitializer.java:25-164` |
| 内置权限/角色目录 | `backend/land/src/main/java/com/skylink/land/service/identity/bootstrap/SecurityBootstrapCatalog.java` |
| 初始管理员创建与校验 | `backend/land/src/main/java/com/skylink/land/service/identity/bootstrap/BootstrapAdminService.java:14-104` |
| 开关与环境变量配置 | `backend/land/src/main/resources/application.yaml` 的 `skylink.bootstrap` 段 |
| 初始 SQL 数据 | `backend/land/src/main/resources/data.sql` |

## 什么时候会运行

代码位置：`SecurityDataInitializer.java:25-27`

```java
@Component // 1. 把这个类交给 Spring 管理。
@ConditionalOnProperty(
    prefix = "skylink.bootstrap", // 2. 配置前缀。
    name = "security-data-enabled", // 3. 完整键：skylink.bootstrap.security-data-enabled。
    havingValue = "true", // 4. 明确设为 true 时运行。
    matchIfMissing = true // 5. 没写该配置时，也默认运行。
)
public class SecurityDataInitializer implements ApplicationRunner {
    // 6. ApplicationRunner 表示 Spring Boot 应用准备就绪后执行 run()。
}
```

如果部署环境明确不想补齐安全数据，可把该开关设为 `false`。但要先确认数据库中的角色和权限已经完整，否则登录用户可能没有可用权限。

## 初始化主流程：按顺序补齐

代码位置：`SecurityDataInitializer.java:57-86`

```java
@Transactional(rollbackFor = Exception.class) // 1. 安全基础数据要么完整补齐，要么整体回滚。
public void run(ApplicationArguments args) {
    Map<String, Permission> permissions = ensurePermissions(); // 2. 先保证权限码存在。
    Role userRole = ensureRole("User", SecurityBootstrapCatalog.ROLE_USER, "Default role for authenticated users");
    Role adminRole = ensureRole("Administrator", SecurityBootstrapCatalog.ROLE_ADMIN, "Manages users, departments, and roles");
    Role projectLeaderRole = ensureRole("Project Leader", SecurityBootstrapCatalog.ROLE_PROJECT_LEADER,
        "Manages project tasks on top of the base user permissions");
    Role superAdminRole = ensureRole("Super Administrator", SecurityBootstrapCatalog.ROLE_SUPER_ADMIN,
        "Has all system permissions"); // 3. 四个内置角色逐一确保存在。

    bindPermissions(userRole, permissions, SecurityBootstrapCatalog.USER_PERMISSION_CODES); // 4. 补普通用户权限。
    bindPermissions(adminRole, permissions, SecurityBootstrapCatalog.ADMIN_PERMISSION_CODES); // 5. 补管理员权限。
    bindPermissions(projectLeaderRole, permissions, SecurityBootstrapCatalog.PROJECT_LEADER_PERMISSION_CODES);
    bindDefaultRoleToUsersMissingRoles(userRole); // 6. 已启用、无角色用户补默认角色。
    // 7. 重新读取全部权限并绑定给超级管理员。
    bindPermissions(superAdminRole, allPermissions, allPermissions.keySet().stream().toList());
    bootstrapAdminService.bootstrap(superAdminRole); // 8. 处理可选的初始超级管理员账号。
}
```

这个方法是“幂等”的方向：多次启动不应该不断新增重复角色或重复角色-权限关系，而是查到已有记录后跳过或补缺。

## 已逻辑删除的权限和角色为什么会恢复

代码位置：`SecurityDataInitializer.java:88-123`

```java
Permission permission = permissionMapper.selectByPermissionCodeIncludingDeleted(definition.code());
// 1. 查询包含逻辑删除的数据，否则会误以为该权限不存在。
if (permission == null) {
    permission = new Permission(); // 2. 真的不存在才新建。
    permission.setPermissionCode(definition.code());
    permissionMapper.insert(permission);
} else if (Integer.valueOf(1).equals(permission.getDeleted())) {
    permissionMapper.restoreSystemPermission(permission.getPermissionId());
    // 3. 编目中仍需要的系统权限被逻辑删了，就恢复，而不是另建同码记录。
    permission.setDeleted(0);
}
```

`ensureRole` 对角色采取同样策略（`SecurityDataInitializer.java:108-123`）。这样既不破坏唯一角色码/权限码，也保证系统内置安全能力不会因误逻辑删除而长期缺失。

## 怎样避免重复绑定角色权限

代码位置：`SecurityDataInitializer.java:125-141`

```java
Set<Long> existingPermissionIds = rolePermissionMapper.selectList(
    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getRoleId())
).stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
// 1. 先收集该角色已有权限 ID，转成 Set 便于判断。

for (String permissionCode : permissionCodes) {
    Permission permission = permissions.get(permissionCode); // 2. 从已确保存在的权限表中取对象。
    if (permission == null || existingPermissionIds.contains(permission.getPermissionId())) {
        continue; // 3. 不存在或已绑定，直接跳过。
    }
    RolePermission relation = new RolePermission();
    relation.setRoleId(role.getRoleId());
    relation.setPermissionId(permission.getPermissionId());
    rolePermissionMapper.insert(relation); // 4. 只插入缺少的关联。
}
```

## 初始超级管理员为什么要严格核验

代码位置：`BootstrapAdminService.java:38-52`、`54-103`

```java
public void bootstrap(Role superAdminRole) {
    properties.validate(); // 1. 先检查配置是否完整、合理。
    if (!properties.isEnabled()) return; // 2. 未启用管理员引导就什么也不创建。

    String username = properties.getUsername().trim();
    User user = userMapper.selectByUsernameIncludingDeleted(username); // 3. 包含已删除账号检查重名。
    if (user == null) {
        user = createAdmin(username); // 4. 不存在时创建，并用 PasswordEncoder 加密密码。
    } else {
        validateExistingUser(user, username); // 5. 已存在时校验邮箱、手机号、状态是否匹配。
    }
    bindSuperAdminRole(user, superAdminRole); // 6. 最终确保其拥有超级管理员角色。
}
```

密码没有明文直接入库：创建时调用 `passwordEncoder.encode(properties.getPassword())`，见 `BootstrapAdminService.java:63-70`。文档、日志和提交中都不应写出真实初始密码；生产环境应通过受保护的环境变量配置。

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 每次启动都会新建一套角色 | 不是，代码按角色码查询，已有则跳过/补齐。 |
| 被逻辑删除的内置权限永远失效 | 当前初始化会恢复目录中仍需要的系统权限和角色。 |
| 所有用户都会被强制改成普通角色 | 只给“已启用且没有任何角色”的用户补默认角色。 |
| 这里是在实现系统配置模块 | 不是；这里只是已实现的启动安全数据引导。 |
| 初始管理员密码会以明文入库 | 不会，创建时经 `PasswordEncoder` 编码。 |

## 人话复盘

启动初始化像安全系统的“自检与补货”：缺少的权限、角色、关系会补上，误逻辑删除的内置项会恢复；需要时再谨慎创建一个初始超级管理员。它只保证 RBAC 的地基完整，不等同于尚未实现的系统配置功能。
