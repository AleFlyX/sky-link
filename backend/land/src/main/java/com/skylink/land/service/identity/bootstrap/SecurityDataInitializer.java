package com.skylink.land.service.identity.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "skylink.bootstrap", name = "security-data-enabled", havingValue = "true", matchIfMissing = true)
public class SecurityDataInitializer implements ApplicationRunner {

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final BootstrapAdminService bootstrapAdminService;

    public SecurityDataInitializer(
        RoleMapper roleMapper,
        PermissionMapper permissionMapper,
        RolePermissionMapper rolePermissionMapper,
        BootstrapAdminService bootstrapAdminService
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.bootstrapAdminService = bootstrapAdminService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Map<String, Permission> permissions = ensurePermissions();
        Role userRole = ensureRole("User", SecurityBootstrapCatalog.ROLE_USER, "Default role for authenticated users");
        Role adminRole = ensureRole(
            "Administrator",
            SecurityBootstrapCatalog.ROLE_ADMIN,
            "Manages users, departments, and roles"
        );
        Role superAdminRole = ensureRole(
            "Super Administrator",
            SecurityBootstrapCatalog.ROLE_SUPER_ADMIN,
            "Has all system permissions"
        );

        bindPermissions(userRole, permissions, SecurityBootstrapCatalog.USER_PERMISSION_CODES);
        bindPermissions(adminRole, permissions, SecurityBootstrapCatalog.ADMIN_PERMISSION_CODES);
        Map<String, Permission> allPermissions = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()).stream()
            .collect(Collectors.toMap(Permission::getPermissionCode, permission -> permission));
        bindPermissions(superAdminRole, allPermissions, allPermissions.keySet().stream().toList());
        bootstrapAdminService.bootstrap(superAdminRole);
    }

    private Map<String, Permission> ensurePermissions() {
        Map<String, Permission> permissions = new LinkedHashMap<>();
        for (SecurityBootstrapCatalog.PermissionDefinition definition : SecurityBootstrapCatalog.PERMISSIONS) {
            Permission permission = permissionMapper.selectByPermissionCodeIncludingDeleted(definition.code());
            if (permission == null) {
                permission = new Permission();
                permission.setPermissionName(definition.name());
                permission.setPermissionCode(definition.code());
                permission.setPermissionType(3);
                permission.setSortNo(definition.sortNo());
                permissionMapper.insert(permission);
            } else if (Integer.valueOf(1).equals(permission.getDeleted())) {
                permissionMapper.restoreSystemPermission(permission.getPermissionId());
                permission.setDeleted(0);
            }
            permissions.put(definition.code(), permission);
        }
        return permissions;
    }

    private Role ensureRole(String name, String code, String description) {
        Role role = roleMapper.selectByRoleCodeIncludingDeleted(code);
        if (role == null) {
            role = new Role();
            role.setRoleName(name);
            role.setRoleCode(code);
            role.setDescription(description);
            role.setStatus(1);
            roleMapper.insert(role);
        } else if (Integer.valueOf(1).equals(role.getDeleted())) {
            roleMapper.restoreSystemRole(role.getRoleId());
            role.setDeleted(0);
            role.setStatus(1);
        }
        return role;
    }

    private void bindPermissions(Role role, Map<String, Permission> permissions, List<String> permissionCodes) {
        Set<Long> existingPermissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getRoleId())
            ).stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toSet());
        for (String permissionCode : permissionCodes) {
            Permission permission = permissions.get(permissionCode);
            if (permission == null || existingPermissionIds.contains(permission.getPermissionId())) {
                continue;
            }
            RolePermission relation = new RolePermission();
            relation.setRoleId(role.getRoleId());
            relation.setPermissionId(permission.getPermissionId());
            rolePermissionMapper.insert(relation);
        }
    }
}
