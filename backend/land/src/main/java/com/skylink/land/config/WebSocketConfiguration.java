package com.skylink.land.config;

import com.skylink.land.websocket.MessageWebSocketHandler;
import com.skylink.land.websocket.WebSocketAuthHandshakeInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
// 开启 Spring 原生 WebSocket 支持；本项目没有使用 STOMP，而是直接处理文本帧。
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;

    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;

    @Value("${skylink.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
    // WebSocket 握手同样受浏览器 Origin 限制；这里复用项目 CORS 来源配置，避免陌生网页建立连接。
    private List<String> allowedOriginPatterns;

    public WebSocketConfiguration(
        MessageWebSocketHandler messageWebSocketHandler,
        WebSocketAuthHandshakeInterceptor authHandshakeInterceptor
    ) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 将消息实时推送端点固定为 /ws/messages，前端 useMessageCenter 会连接到此地址。
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
            // HTTP 升级为 WebSocket 前先校验 JWT，并把可信用户信息写入 WebSocketSession attributes。
            .addInterceptors(authHandshakeInterceptor)
            // 浏览器来源不在白名单时，握手阶段就会被拒绝。
            .setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
    }
}
