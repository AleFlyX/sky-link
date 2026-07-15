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
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;

    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;

    @Value("${skylink.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
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
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
            .addInterceptors(authHandshakeInterceptor)
            .setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
    }
}
