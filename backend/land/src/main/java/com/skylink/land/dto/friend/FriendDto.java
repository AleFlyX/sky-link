package com.skylink.land.dto.friend;

import com.skylink.land.dto.common.PageRequest;
import com.skylink.land.dto.user.UserDto;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FriendDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFriendRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long friendUserId;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendRequestResultResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long requestId;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandleFriendRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendItemResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long friendId;
        private UserDto.UserSummaryResponse friendUser;
        private LocalDateTime addTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandleFriendResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long friendId;
        private UserDto.UserSummaryResponse friendUser;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class FriendListQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String nickname;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class FriendRequestQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendRequestItemResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long requestId;
        private UserDto.UserSummaryResponse requestUser;
        private String message;
        private String status;
        private LocalDateTime requestTime;
    }
}
