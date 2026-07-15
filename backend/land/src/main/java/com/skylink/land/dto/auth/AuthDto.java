package com.skylink.land.dto.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String username;
        private String password;
        private String nickname;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String account;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String accessToken;
        private String token;
        private Long expiresIn;
        private LoginUserInfo userInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUserInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private Long departmentId;
        private Integer status;
        private List<String> roles;
        private List<String> permissions;
    }
}
