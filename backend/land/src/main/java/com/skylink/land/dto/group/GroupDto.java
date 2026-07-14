package com.skylink.land.dto.group;

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
public final class GroupDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String groupName;
        private String notice;
        private List<Long> memberIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGroupRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String groupName;
        private String notice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class GroupListQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSummaryResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long groupId;
        private String groupName;
        private String notice;
        private Long ownerId;
        private String ownerName;
        private Integer memberCount;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupDetailResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long groupId;
        private String groupName;
        private String notice;
        private Long ownerId;
        private String ownerName;
        private Integer memberCount;
        private LocalDateTime createTime;
        private List<GroupMemberResponse> members;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String role;
        private LocalDateTime joinTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class GroupMemberQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InviteGroupMembersRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private List<Long> userIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGroupMemberRoleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String role;
    }
}
