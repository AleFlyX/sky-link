package com.skylink.land.websocket;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MessageWebSocketSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(WebSocketSession session) {
        Object userIdValue = session.getAttributes().get(WebSocketSessionKeys.USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            return;
        }

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId, sessions);
        }
    }

    public void sendToUsers(Collection<Long> userIds, String payload) {
        for (Long userId : userIds) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }

            for (WebSocketSession session : sessions.toArray(WebSocketSession[]::new)) {
                if (!session.isOpen()) {
                    unregister(session);
                    continue;
                }
                try {
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(payload));
                        }
                    }
                } catch (IOException exception) {
                    unregister(session);
                }
            }
        }
    }
}
