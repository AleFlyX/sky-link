package com.skylink.land.collaboration;

import java.time.Instant;

public record CollaborationTicketClaims(
    Long userId, Long documentId, String permission, String displayName,
    String ticketId, Instant issuedAt, Instant expiresAt
) {}
