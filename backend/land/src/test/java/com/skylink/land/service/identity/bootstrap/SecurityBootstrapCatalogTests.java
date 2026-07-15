package com.skylink.land.service.identity.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skylink.land.auth.RequirePermission;
import com.skylink.land.controller.DepartmentController;
import com.skylink.land.controller.DocumentController;
import com.skylink.land.controller.PermissionController;
import com.skylink.land.controller.RoleController;
import com.skylink.land.controller.TaskController;
import com.skylink.land.controller.UserController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SecurityBootstrapCatalogTests {

    @Test
    void bootstrapPermissionsCoverAllControllerPermissionAnnotations() {
        Set<String> declaredPermissions = SecurityBootstrapCatalog.PERMISSIONS.stream()
            .map(SecurityBootstrapCatalog.PermissionDefinition::code)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> controllerPermissions = Arrays.stream(new Class<?>[] {
            UserController.class,
            DepartmentController.class,
            RoleController.class,
            PermissionController.class,
            DocumentController.class,
            TaskController.class
        }).flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
            .map(method -> method.getAnnotation(RequirePermission.class))
            .filter(annotation -> annotation != null)
            .flatMap(annotation -> Arrays.stream(annotation.value()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> missingPermissions = new LinkedHashSet<>(controllerPermissions);
        missingPermissions.removeAll(declaredPermissions);

        assertTrue(
            missingPermissions.isEmpty(),
            "Security bootstrap is missing permissions required by controllers: " + missingPermissions
        );
    }

    @Test
    void defaultUserRoleIncludesDocumentAndTaskPermissions() {
        Set<String> userPermissions = new LinkedHashSet<>(SecurityBootstrapCatalog.USER_PERMISSION_CODES);

        Set<String> expectedPermissions = Set.of(
            "document:create",
            "document:list",
            "document:get",
            "document:update",
            "document:delete",
            "document:permission:user:set",
            "document:permission:group:set",
            "document:permission:list",
            "document:permission:user:delete",
            "document:permission:group:delete",
            "task:list",
            "task:get",
            "task:status:update"
        );

        assertTrue(
            userPermissions.containsAll(expectedPermissions),
            "ROLE_USER is missing default collaboration permissions"
        );
    }

    @Test
    void adminRoleIncludesPermissionManagementAndTaskWritePermissions() {
        Set<String> adminPermissions = new LinkedHashSet<>(SecurityBootstrapCatalog.ADMIN_PERMISSION_CODES);

        Set<String> expectedPermissions = Set.of(
            "permission:list",
            "permission:create",
            "permission:update",
            "permission:delete",
            "task:create",
            "task:update",
            "task:delete"
        );

        assertTrue(
            adminPermissions.containsAll(expectedPermissions),
            "ROLE_ADMIN is missing expected management permissions"
        );
    }
}
