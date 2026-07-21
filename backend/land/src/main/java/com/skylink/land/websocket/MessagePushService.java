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
            // 没有接收人时不进行 JSON 序列化和网络循环，避免无意义工作。
            return;
        }

        // 前端约定收到 { type, message, session }：type 区分新消息/撤回，session 用于刷新会话列表预览。
        MessageDto.MessageRealtimeEvent event = MessageDto.MessageRealtimeEvent.builder()
            .type(type)
            .message(message)
            .session(session)
            .build();
        try {
            // 先序列化为一段 JSON 文本，再由注册表复制给每位接收人的全部在线连接。
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
        // 单人版本只是集合版本的便捷包装，保持两种调用的事件结构完全一致。
        push(type, message, session, List.of(recipientId));
    }
}
