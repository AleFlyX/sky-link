package com.skylink.land.auth;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "skylink.jwt")
public class JwtProperties {

    private String issuer = "sky-link";

    private String secret = "change-me-to-a-long-random-secret-at-least-32-bytes";

    private Duration ttl = Duration.ofHours(24);

    private String header = "Authorization";

    private String tokenPrefix = "Bearer ";

    private List<String> excludePaths = new ArrayList<>(List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/health",
        "/error"
    ));
}
