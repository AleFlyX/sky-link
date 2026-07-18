package com.skylink.land.auth;

import com.skylink.land.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtProperties properties;

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationInterceptor(JwtProperties properties, JwtTokenProvider tokenProvider) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            // 浏览器跨域预检不携带业务 Token；先放行，实际业务请求再做认证。
            return true;
        }

        String authorization = request.getHeader(properties.getHeader());
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(properties.getTokenPrefix())) {
            // 没有 Bearer 前缀或根本没带请求头，不能继续访问受保护接口。
            throw new UnauthorizedException("请先登录");
        }

        String token = authorization.substring(properties.getTokenPrefix().length()).trim();
        // parseAccessToken 会同时验证签名、签发方、token 类型和过期时间。
        JwtClaims claims = tokenProvider.parseAccessToken(token);
        // 认证通过后把“当前是谁”放到本请求的上下文，Controller/Service 不再信任前端 userId。
        AuthContext.setCurrentUser(tokenProvider.toAuthenticatedUser(claims));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程会被服务器复用，结束时必须清理 ThreadLocal，防止下一个请求误读前一个人的身份。
        AuthContext.clear();
    }
}
