package com.skylink.land.websocket;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final MessageWebSocketSessionRegistry sessionRegistry;

    public MessageWebSocketHandler(MessageWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdValue = session.getAttributes().get(WebSocketSessionKeys.USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionRegistry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // The client only keeps the socket open for server pushes.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(session);
        super.handleTransportError(session, exception);
    }
}
