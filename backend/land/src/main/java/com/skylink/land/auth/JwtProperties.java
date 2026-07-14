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

    private String header = "Authorization";

    private String tokenPrefix = "Bearer ";

    private List<String> excludePaths = new ArrayList<>(List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
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
    }
}
