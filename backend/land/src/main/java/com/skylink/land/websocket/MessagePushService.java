package com.skylink.land.websocket;

import com.skylink.land.dto.message.MessageDto;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class MessagePushService {

    private final ObjectMapper objectMapper;

    private final MessageWebSocketSessionRegistry sessionRegistry;

    public MessagePushService(ObjectMapper objectMapper, MessageWebSocketSessionRegistry sessionRegistry) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
    }

    public void push(
        String type,
        MessageDto.MessageResponse message,
        MessageDto.MessageSessionResponse session,
        Collection<Long> recipientIds
    ) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }

        MessageDto.MessageRealtimeEvent event = MessageDto.MessageRealtimeEvent.builder()
            .type(type)
            .message(message)
            .session(session)
            .build();
        try {
            sessionRegistry.sendToUsers(recipientIds, objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize websocket message", exception);
        }
    }

    public void push(
        String type,
        MessageDto.MessageResponse message,
        MessageDto.MessageSessionResponse session,
        Long recipientId
    ) {
        push(type, message, session, List.of(recipientId));
    }
}
