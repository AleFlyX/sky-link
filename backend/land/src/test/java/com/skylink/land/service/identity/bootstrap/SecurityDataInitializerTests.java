package com.skylink.land.service.identity.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityDataInitializerTests {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private BootstrapAdminService bootstrapAdminService;

    @Test
    void restoresSoftDeletedSecurityDataAndContinuesBootstrap() {
        AtomicLong permissionIds = new AtomicLong(1);
        Map<String, Permission> permissions = new LinkedHashMap<>();
        when(permissionMapper.selectByPermissionCodeIncludingDeleted(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            Permission permission = new Permission();
            permission.setPermissionId(permissionIds.getAndIncrement());
            permission.setPermissionCode(code);
            permission.setDeleted("user:me:get".equals(code) ? 1 : 0);
            permissions.put(code, permission);
            return permission;
        });
        when(permissionMapper.selectList(any())).thenAnswer(invocation -> List.copyOf(permissions.values()));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of());
        User orphanUser = new User();
        orphanUser.setUserId(99L);
        orphanUser.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(orphanUser));
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(roleMapper.selectByRoleCodeIncludingDeleted(anyString())).thenAnswer(invocation -> {
            Role role = new Role();
            role.setRoleId((long) invocation.getArgument(0, String.class).hashCode() & Integer.MAX_VALUE);
            role.setRoleCode(invocation.getArgument(0));
            role.setDeleted(SecurityBootstrapCatalog.ROLE_USER.equals(role.getRoleCode()) ? 1 : 0);
            role.setStatus(1);
            return role;
        });

        SecurityDataInitializer initializer = new SecurityDataInitializer(
            roleMapper,
            permissionMapper,
            rolePermissionMapper,
            userMapper,
            userRoleMapper,
            bootstrapAdminService
        );
        initializer.run(null);

        verify(permissionMapper).restoreSystemPermission(1L);
        verify(roleMapper).restoreSystemRole(any());
        verify(userRoleMapper).insert(any(UserRole.class));
        verify(bootstrapAdminService).bootstrap(any(Role.class));
    }
}
