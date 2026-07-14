package com.skylink.land.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skylink.land.exception.UnauthorizedException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JwtTokenProviderTests {

    @Test
    void accessAndRefreshTokensAreNotInterchangeable() {
        JwtTokenProvider provider = new JwtTokenProvider(properties(), new ObjectMapper());

        String accessToken = provider.generateToken(1L, "demo", List.of("ROLE_USER"));
        String refreshToken = provider.generateRefreshToken(1L, "demo", List.of("ROLE_USER"));

        assertThat(provider.parseAccessToken(accessToken).getTokenType()).isEqualTo(JwtTokenProvider.ACCESS_TOKEN_TYPE);
        assertThat(provider.parseRefreshToken(refreshToken).getTokenType()).isEqualTo(JwtTokenProvider.REFRESH_TOKEN_TYPE);
        assertThatThrownBy(() -> provider.parseAccessToken(refreshToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Token 类型无效");
        assertThatThrownBy(() -> provider.parseRefreshToken(accessToken))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Token 类型无效");
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setTtl(Duration.ofMinutes(15));
        properties.setRefreshTtl(Duration.ofDays(7));
        return properties;
    }
}
