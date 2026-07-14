package com.skylink.land.service.identity.bootstrap;

import java.util.List;
import java.util.stream.Stream;

public final class SecurityBootstrapCatalog {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String ROLE_USER = "ROLE_USER";

    static final List<PermissionDefinition> PERMISSIONS = List.of(
        permission("Get Current User Profile", "user:me:get", 10),
        permission("Update Current User Profile", "user:me:update", 11),
        permission("Update Current User Password", "user:password:update", 12),
        permission("List Users", "user:list", 13),
        permission("Get User Detail", "user:get", 14),
        permission("Update User Status", "user:status:update", 15),
        permission("Delete User", "user:delete", 16),
        permission("Assign User Roles", "user:role:add", 17),
        permission("Remove User Role", "user:role:delete", 18),
        permission("List Departments", "department:list", 30),
        permission("Create Department", "department:create", 31),
        permission("Update Department", "department:update", 32),
        permission("Delete Department", "department:delete", 33),
        permission("List Department Members", "department:members:list", 34),
        permission("List Roles", "role:list", 40),
        permission("Create Role", "role:create", 41),
        permission("Update Role", "role:update", 42),
        permission("Delete Role", "role:delete", 43),
        permission("Set Role Permissions", "role:permission:set", 44),
        permission("List Permissions", "permission:list", 50),
        permission("Create Permission", "permission:create", 51),
        permission("Update Permission", "permission:update", 52),
        permission("Delete Permission", "permission:delete", 53)
    );

    static final List<String> USER_PERMISSION_CODES = List.of(
        "user:me:get",
        "user:me:update",
        "user:password:update",
        "user:get",
        "department:list",
        "department:members:list"
    );

    static final List<String> ADMIN_PERMISSION_CODES = concat(USER_PERMISSION_CODES, List.of(
        "user:list",
        "user:status:update",
        "user:delete",
        "user:role:add",
        "user:role:delete",
        "department:create",
        "department:update",
        "department:delete",
        "role:list",
        "role:create",
        "role:update",
        "role:delete",
        "role:permission:set"
    ));

    private SecurityBootstrapCatalog() {
    }

    private static PermissionDefinition permission(String name, String code, int sortNo) {
        return new PermissionDefinition(name, code, sortNo);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).distinct().toList();
    }

    record PermissionDefinition(String name, String code, int sortNo) {
    }
}
