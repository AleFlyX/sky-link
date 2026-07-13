package com.skylink.land.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {

    private Long userId;

    private String username;

    @Builder.Default
    private List<String> roles = Collections.emptyList();

    private Instant issuedAt;

    private Instant expiresAt;
}
