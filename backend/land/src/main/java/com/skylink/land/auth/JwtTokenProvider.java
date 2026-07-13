package com.skylink.land.auth;

import com.skylink.land.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final JwtProperties properties;

    private final ObjectMapper objectMapper;

    public JwtTokenProvider(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
            "alg", "HS256",
            "typ", "JWT"
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("sub", String.valueOf(userId));
        payload.put("username", username);
        payload.put("roles", CollectionUtils.isEmpty(roles) ? List.of() : roles);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(properties.getTtl()).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public JwtClaims parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Token 不能为空");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("Token 格式错误");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new UnauthorizedException("Token 签名无效");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        if (!properties.getIssuer().equals(payload.get("iss"))) {
            throw new UnauthorizedException("Token 签发方无效");
        }

        Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
        if (Instant.now().isAfter(expiresAt)) {
            throw new UnauthorizedException("Token 已过期");
        }

        return JwtClaims.builder()
            .userId(Long.valueOf(String.valueOf(payload.get("sub"))))
            .username(String.valueOf(payload.get("username")))
            .roles(asStringList(payload.get("roles")))
            .issuedAt(Instant.ofEpochSecond(asLong(payload.get("iat"))))
            .expiresAt(expiresAt)
            .build();
    }

    public AuthenticatedUser toAuthenticatedUser(JwtClaims claims) {
        return AuthenticatedUser.builder()
            .userId(claims.getUserId())
            .username(claims.getUsername())
            .roles(claims.getRoles())
            .build();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode token json", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] decoded = BASE64_URL_DECODER.decode(value);
            return objectMapper.readValue(decoded, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new UnauthorizedException("Token 内容无效");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
