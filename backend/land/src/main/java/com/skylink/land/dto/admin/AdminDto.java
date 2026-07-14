package com.skylink.land.dto.admin;

import com.skylink.land.dto.common.PageRequest;
import com.skylink.land.dto.user.UserDto;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;
        private String roleName;
        private String roleCode;
        private String description;
        private Integer status;
        private List<PermissionResponse> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String roleName;
        private String roleCode;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRoleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String roleName;
        private String roleCode;
        private String description;
        private Integer status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRolePermissionsRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> permissionCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long permissionId;
        private String permissionName;
        private String permissionCode;
        private Integer permissionType;
        private Long parentId;
        private Integer sortNo;
        private List<PermissionResponse> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class LoginLogQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private LocalDateTime loginTimeStart;
        private LocalDateTime loginTimeEnd;
        private Integer loginResult;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginLogResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long logId;
        private Long userId;
        private String username;
        private String loginIp;
        private String device;
        private String browser;
        private Integer loginResult;
        private LocalDateTime loginTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class OperationLogQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String module;
        private String operation;
        private LocalDateTime operationTimeStart;
        private LocalDateTime operationTimeEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationLogResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long operationId;
        private Long userId;
        private String username;
        private String module;
        private String operation;
        private String requestUrl;
        private String requestMethod;
        private LocalDateTime operationTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsOverviewResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer totalUsers;
        private Integer onlineUsers;
        private Integer totalGroups;
        private Integer totalMessages;
        private Integer totalFiles;
        private Double taskCompletionRate;
        private List<DepartmentStatResponse> departmentStats;
        private List<RecentMessageStatResponse> recentMessages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStatResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long departmentId;
        private String departmentName;
        private Integer memberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentMessageStatResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private LocalDate date;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemConfigResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long configId;
        private String configKey;
        private String configValue;
        private String description;
        private UserDto.UserSummaryResponse updater;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSystemConfigRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String configValue;
        private String description;
    }
}
