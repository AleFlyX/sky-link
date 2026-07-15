package com.skylink.land.dto.user;

import com.skylink.land.dto.common.PageRequest;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long roleId;
        private String roleName;
        private String roleCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private Integer status;
        private Long departmentId;
        private String departmentName;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private Integer status;
        private Long departmentId;
        private String departmentName;
        private LocalDateTime createTime;
        private List<UserRoleInfo> roles;
        private List<String> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String nickname;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String oldPassword;
        private String newPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class UserQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String username;
        private String nickname;
        private Long departmentId;
        private Integer status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserStatusRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignUserRolesRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private List<Long> roleIds;
    }
}
