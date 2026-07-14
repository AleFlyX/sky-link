package com.skylink.land.auth;

import com.skylink.land.dto.auth.AuthDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPair {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private AuthDto.LoginUserInfo userInfo;

    public AuthDto.TokenResponse toResponse() {
        return AuthDto.TokenResponse.builder()
            .accessToken(accessToken)
            .token(accessToken)
            .expiresIn(expiresIn)
            .userInfo(userInfo)
            .build();
    }
}
