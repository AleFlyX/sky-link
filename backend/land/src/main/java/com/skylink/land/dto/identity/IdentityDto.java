package com.skylink.land.dto.identity;

import com.skylink.land.dto.common.PageRequest;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentityDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveUserRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String username;
        private String password;
        private String nickname;
        private String email;
        private String phone;
        private Integer status;
        private Long departmentId;
        private List<Long> roleIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveRoleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String roleName;
        private String roleCode;
        private String description;
        private Integer status;
        private List<Long> permissionIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavePermissionRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String permissionName;
        private String permissionCode;
        private Integer permissionType;
        private Long parentId;
        private Integer sortNo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class RoleQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String roleName;
        private String roleCode;
        private Integer status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class PermissionQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String permissionName;
        private String permissionCode;
        private Integer permissionType;
        private Long parentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignUserRolesRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private List<Long> roleIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRolePermissionsRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;
        private List<Long> permissionIds;
    }
}
