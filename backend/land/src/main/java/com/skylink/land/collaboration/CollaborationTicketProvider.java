package com.skylink.land.collaboration;

import com.skylink.land.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class CollaborationTicketProvider {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final CollaborationProperties properties;
    private final ObjectMapper objectMapper;

    public CollaborationTicketProvider(CollaborationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties; this.objectMapper = objectMapper;
    }

    public CollaborationTicket issue(Long userId, Long documentId, String permission, String displayName) {
        validateSecret();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getTicketTtl());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.getIssuer()); payload.put("aud", properties.getAudience());
        payload.put("sub", String.valueOf(userId)); payload.put("documentId", documentId);
        payload.put("permission", permission); payload.put("displayName", displayName);
        payload.put("jti", UUID.randomUUID().toString()); payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
        String body = encode(payload); String unsigned = header + "." + body;
        return new CollaborationTicket(unsigned + "." + sign(unsigned), expiresAt);
    }

    public CollaborationTicketClaims parse(String token) {
        validateSecret();
        if (!StringUtils.hasText(token)) throw invalid();
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !constantTimeEquals(sign(parts[0] + "." + parts[1]), parts[2])) throw invalid();
        try {
            Map<String, Object> header = objectMapper.readValue(DECODER.decode(parts[0]), new TypeReference<>() {});
            if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) throw invalid();
            Map<String, Object> p = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            if (!properties.getIssuer().equals(p.get("iss")) || !properties.getAudience().equals(p.get("aud"))) throw invalid();
            Long userId = Long.valueOf(String.valueOf(p.get("sub"))); long documentId = asLong(p.get("documentId"));
            String permission = String.valueOf(p.get("permission")); String displayName = String.valueOf(p.get("displayName"));
            String ticketId = String.valueOf(p.get("jti")); Instant issuedAt = Instant.ofEpochSecond(asLong(p.get("iat")));
            Instant expiresAt = Instant.ofEpochSecond(asLong(p.get("exp"))); Instant now = Instant.now();
            if (userId < 1 || documentId < 1 || !List.of("read", "edit", "manage").contains(permission)
                || !StringUtils.hasText(displayName) || !StringUtils.hasText(ticketId) || "null".equals(ticketId)
                || issuedAt.isAfter(now.plusSeconds(30)) || !expiresAt.isAfter(now) || !expiresAt.isAfter(issuedAt)) throw invalid();
            return new CollaborationTicketClaims(userId, documentId, permission, displayName, ticketId, issuedAt, expiresAt);
        } catch (UnauthorizedException exception) { throw exception; }
        catch (Exception exception) { throw invalid(); }
    }

    private String encode(Map<String, Object> value) {
        try { return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value)); }
        catch (Exception exception) { throw new IllegalStateException("failed to encode collaboration ticket", exception); }
    }
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getTicketSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("failed to sign collaboration ticket", exception); }
    }
    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    private long asLong(Object value) { return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value)); }
    private void validateSecret() {
        if (!StringUtils.hasText(properties.getTicketSecret()) || properties.getTicketSecret().length() < 32)
            throw new IllegalStateException("collaboration ticket secret must contain at least 32 characters");
    }
    private UnauthorizedException invalid() { return new UnauthorizedException("协同票据无效或已过期"); }
}
