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

    // key 是用户 ID，value 是该用户全部已连接标签页；并发容器适合 HTTP 线程和 WebSocket 线程同时访问。
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        // 第一次连接时创建并发 Set；之后的多标签页连接加入同一个用户集合。
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(WebSocketSession session) {
        // 用户 ID 来自握手后保存的 attributes，而不是客户端断开时提供的任意数据。
        Object userIdValue = session.getAttributes().get(WebSocketSessionKeys.USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            return;
        }

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        // 只移除当前这条连接，不影响同一用户仍在其他标签页打开的会话。
        sessions.remove(session);
        if (sessions.isEmpty()) {
            // 最后一条连接离开后删除 key，防止在线表长时间积累空集合。
            userSessions.remove(userId, sessions);
        }
    }

    public void sendToUsers(Collection<Long> userIds, String payload) {
        // 同一条事件可能推给单聊双方或所有群成员；逐个用户查出其全部在线标签页。
        for (Long userId : userIds) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }

            for (WebSocketSession session : sessions.toArray(WebSocketSession[]::new)) {
                // 复制为数组后遍历，避免推送过程中其他线程增删 Set 导致遍历不稳定。
                if (!session.isOpen()) {
                    unregister(session);
                    continue;
                }
                try {
                    // Spring 的单个 WebSocketSession 不允许并发 sendMessage；锁住该 session 防止多个业务线程交错写帧。
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(payload));
                        }
                    }
                } catch (IOException exception) {
                    // 写入失败通常说明网络已断；清理后下次推送不会反复尝试此会话。
                    unregister(session);
                }
            }
        }
    }
}
