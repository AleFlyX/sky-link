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
        permission("Delete Permission", "permission:delete", 53),
        permission("Create Document", "document:create", 60),
        permission("List Documents", "document:list", 61),
        permission("Get Document Detail", "document:get", 62),
        permission("Update Document", "document:update", 63),
        permission("Delete Document", "document:delete", 64),
        permission("Set User Document Permission", "document:permission:user:set", 65),
        permission("Set Group Document Permission", "document:permission:group:set", 66),
        permission("List Document Permissions", "document:permission:list", 67),
        permission("Remove User Document Permission", "document:permission:user:delete", 68),
        permission("Remove Group Document Permission", "document:permission:group:delete", 69),
        permission("Create Task", "task:create", 70),
        permission("List Tasks", "task:list", 71),
        permission("Get Task Detail", "task:get", 72),
        permission("Update Task", "task:update", 73),
        permission("Update Task Status", "task:status:update", 74),
        permission("Delete Task", "task:delete", 75)
    );

    static final List<String> USER_PERMISSION_CODES = List.of(
        "user:me:get",
        "user:me:update",
        "user:password:update",
        "user:get",
        "department:list",
        "department:members:list",
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
        "role:permission:set",
        "permission:list",
        "permission:create",
        "permission:update",
        "permission:delete",
        "task:create",
        "task:update",
        "task:delete"
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
