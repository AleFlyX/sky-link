package com.skylink.land.dto.notice;

import com.skylink.land.dto.common.PageRequest;
import com.skylink.land.dto.user.UserDto;
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
public final class NoticeDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNoticeRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private String targetType;
        private List<Long> targetIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class NoticeQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoticeResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long noticeId;
        private String title;
        private String content;
        private String targetType;
        private List<Long> targetIds;
        private Integer status;
        private LocalDateTime publishTime;
        private LocalDateTime createTime;
        private UserDto.UserSummaryResponse publisher;
        private Boolean read;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoticeUnreadCountResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer unreadCount;
    }
}
