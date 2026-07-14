package com.skylink.land.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "skylink.jwt")
public class JwtProperties implements InitializingBean {

    private static final int MIN_SECRET_BYTES = 32;

    private static final String INSECURE_DEFAULT_SECRET = "change-me-to-a-long-random-secret-at-least-32-bytes";

    private String issuer = "sky-link";

    @ToString.Exclude
    private String secret;

    private Duration ttl = Duration.ofHours(24);

    private Duration refreshTtl = Duration.ofDays(7);

    private String header = "Authorization";

    private String tokenPrefix = "Bearer ";

    private RefreshCookie refreshCookie = new RefreshCookie();

    private List<String> excludePaths = new ArrayList<>(List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/health",
        "/error"
    ));

    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        if (INSECURE_DEFAULT_SECRET.equals(secret)
            || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes and must not use the example value");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("JWT token ttl must be positive");
        }
        if (refreshTtl == null || refreshTtl.isZero() || refreshTtl.isNegative()) {
            throw new IllegalStateException("JWT refresh token ttl must be positive");
        }
        if (!refreshTtl.minus(ttl).isPositive()) {
            throw new IllegalStateException("JWT refresh token ttl must be longer than access token ttl");
        }
        refreshCookie.validate();
    }

    @Data
    public static class RefreshCookie {

        private String name = "skylink_refresh_token";

        private String path = "/api/v1/auth";

        private String domain;

        private boolean httpOnly = true;

        private boolean secure = false;

        private String sameSite = "Lax";

        public void validate() {
            if (!StringUtils.hasText(name)) {
                throw new IllegalStateException("JWT refresh cookie name must be configured");
            }
            if (!StringUtils.hasText(path)) {
                throw new IllegalStateException("JWT refresh cookie path must be configured");
            }
            if (!List.of("Strict", "Lax", "None").contains(sameSite)) {
                throw new IllegalStateException("JWT refresh cookie same-site must be Strict, Lax or None");
            }
            if ("None".equals(sameSite) && !secure) {
                throw new IllegalStateException("JWT refresh cookie with SameSite=None must be secure");
            }
        }
    }
}
