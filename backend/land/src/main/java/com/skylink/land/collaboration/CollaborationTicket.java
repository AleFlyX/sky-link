package com.skylink.land.collaboration;

import java.time.Instant;

public record CollaborationTicket(String token, Instant expiresAt) {}
