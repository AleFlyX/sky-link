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
            // 浏览器 WebSocket API 不方便像 Axios 一样统一加 Authorization 头，因此也支持从 ?token= 读取。
            String token = extractToken(request);
            if (!StringUtils.hasText(token)) {
                // 返回 false 会终止 HTTP -> WebSocket 升级；客户端只能收到连接失败，不能得到一个匿名会话。
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 与普通接口同样验证 JWT 签名、签发方、过期时间等，不能只做字符串存在性检查。
            JwtClaims claims = jwtTokenProvider.parseToken(token);
            // attributes 会复制到后续 WebSocketSession；Handler 从这里取得“当前连接属于谁”。
            attributes.put(WebSocketSessionKeys.USER_ID, claims.getUserId());
            attributes.put(WebSocketSessionKeys.USERNAME, claims.getUsername());
            return true;
        } catch (UnauthorizedException exception) {
            // 无效/过期 Token 和缺 Token 使用同样的 401 结果，避免暴露令牌校验细节。
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
            // 若运行环境或客户端支持自定义头，优先采用标准 Authorization: Bearer <token> 格式。
            return authorization.substring(jwtProperties.getTokenPrefix().length()).trim();
        }
        // 原生浏览器 WebSocket 常用 URL 查询参数传 token；该值必须全程使用 HTTPS/WSS，避免被网络窃听。
        return UriComponentsBuilder.fromUri(request.getURI())
            .build()
            .getQueryParams()
            .getFirst("token");
    }
}
