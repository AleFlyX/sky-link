package com.skylink.land.dto.document;

import com.skylink.land.dto.common.PageRequest;
import com.skylink.land.dto.user.UserDto;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDocumentRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private String status;
        private Long teamId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDocumentRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private String status;
        private Long teamId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class DocumentQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummaryResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long documentId;
        private String title;
        private String status;
        private Long creatorId;
        private String creatorName;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private String permission;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDetailResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long documentId;
        private String title;
        private String content;
        private String status;
        private Long creatorId;
        private String creatorName;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private List<DocumentPermissionResponse> permissions;
        private String permission;
        private boolean collaborative;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantDocumentPermissionRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private List<Long> userIds;
        private String permissionType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentPermissionResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long documentId;
        private Long userId;
        private String permissionType;
        private LocalDateTime createTime;
        private UserDto.UserSummaryResponse user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantDocumentGroupPermissionRequest implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String permissionType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentGroupPermissionResponse implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long documentId;
        private Long groupId;
        private String permissionType;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentPermissionListResponse implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private List<DocumentPermissionResponse> users;
        private List<DocumentGroupPermissionResponse> groups;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CollaborationTicketResponse implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String token;
        private String websocketUrl;
        private Instant expiresAt;
        private String permission;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CollaborationAuthorizationRequest implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long userId;
        private Long documentId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CollaborationAuthorizationResponse implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private boolean allowed;
        private String permission;
        private String displayName;
        private String documentStatus;
    }

}
