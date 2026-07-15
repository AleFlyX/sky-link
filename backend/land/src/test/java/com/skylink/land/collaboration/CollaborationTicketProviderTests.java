package com.skylink.land.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skylink.land.exception.UnauthorizedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CollaborationTicketProviderTests {
    @Test
    void ticketIsScopedToOneDocumentAndPermission() {
        CollaborationProperties properties = properties();
        CollaborationTicketProvider provider = new CollaborationTicketProvider(properties, new ObjectMapper());

        CollaborationTicket ticket = provider.issue(7L, 42L, "edit", "Alice");
        CollaborationTicketClaims claims = provider.parse(ticket.token());

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.documentId()).isEqualTo(42L);
        assertThat(claims.permission()).isEqualTo("edit");
        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void tamperedTicketIsRejected() {
        CollaborationTicketProvider provider = new CollaborationTicketProvider(properties(), new ObjectMapper());
        String token = provider.issue(7L, 42L, "read", "Alice").token();
        assertThatThrownBy(() -> provider.parse(token.substring(0, token.length() - 2) + "aa"))
            .isInstanceOf(UnauthorizedException.class);
    }

    private CollaborationProperties properties() {
        CollaborationProperties properties = new CollaborationProperties();
        properties.setTicketSecret("01234567890123456789012345678901");
        properties.setTicketTtl(Duration.ofSeconds(60));
        properties.setWebsocketUrl("ws://127.0.0.1:8180");
        properties.setServiceToken("service-token-at-least-32-characters");
        return properties;
    }
}
