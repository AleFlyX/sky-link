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
        // 只有握手拦截器验签成功才会放入 USER_ID；这里再次检查，防止配置漏拦截时产生匿名连接。
        Object userIdValue = session.getAttributes().get(WebSocketSessionKeys.USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            // POLICY_VIOLATION 表示客户端违反服务端连接策略；不把无身份连接加入在线表。
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        // 一个用户可能开多个浏览器标签页，因此注册表按 userId 保存多个 session。
        sessionRegistry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 当前消息 WebSocket 是“服务端单向推送”通道：发送消息仍走受事务保护的 HTTP API。
        // 所以客户端发来的文本帧不解析、不写库，也不会借此绕过 MessageService 的好友/群成员校验。
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 正常关闭时立即移除会话，避免后续推送仍尝试写入已关闭连接。
        sessionRegistry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 网络中断也会触发清理；unregister 是幂等的，所以随后正常 close 再调用也安全。
        sessionRegistry.unregister(session);
        super.handleTransportError(session, exception);
    }
}
