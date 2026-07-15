package com.skylink.land.websocket;

import com.skylink.land.auth.JwtClaims;
import com.skylink.land.auth.JwtProperties;
import com.skylink.land.auth.JwtTokenProvider;
import com.skylink.land.exception.UnauthorizedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProperties jwtProperties;

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthHandshakeInterceptor(JwtProperties jwtProperties, JwtTokenProvider jwtTokenProvider) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        try {
            String token = extractToken(request);
            if (!StringUtils.hasText(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            JwtClaims claims = jwtTokenProvider.parseToken(token);
            attributes.put(WebSocketSessionKeys.USER_ID, claims.getUserId());
            attributes.put(WebSocketSessionKeys.USERNAME, claims.getUsername());
            return true;
        } catch (UnauthorizedException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(jwtProperties.getHeader());
        if (StringUtils.hasText(authorization) && authorization.startsWith(jwtProperties.getTokenPrefix())) {
            return authorization.substring(jwtProperties.getTokenPrefix().length()).trim();
        }
        return UriComponentsBuilder.fromUri(request.getURI())
            .build()
            .getQueryParams()
            .getFirst("token");
    }
}
