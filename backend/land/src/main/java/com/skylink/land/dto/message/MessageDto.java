package com.skylink.land.dto.message;

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
public final class MessageDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long receiverId;
        private Long groupId;
        private String messageType;
        private String content;
        private Long fileId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long messageId;
        private Long senderId;
        private String senderName;
        private Long receiverId;
        private Long groupId;
        private String messageType;
        private String content;
        private Long fileId;
        private Boolean recalled;
        private Boolean read;
        private LocalDateTime sendTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSessionResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String sessionType;
        private Long targetId;
        private String targetName;
        private String targetAvatar;
        private MessageResponse lastMessage;
        private Integer unreadCount;
        private LocalDateTime lastTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class MessageHistoryQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long receiverId;
        private Long groupId;
        private Long before;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkReadRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long receiverId;
        private Long groupId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkReadResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer readCount;
        private List<Long> messageIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer totalUnread;
    }
}
