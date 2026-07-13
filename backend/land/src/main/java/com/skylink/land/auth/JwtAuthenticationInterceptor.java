package com.skylink.land.auth;

import com.skylink.land.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
        String authorization = request.getHeader(properties.getHeader());
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(properties.getTokenPrefix())) {
            throw new UnauthorizedException("请先登录");
        }

        String token = authorization.substring(properties.getTokenPrefix().length()).trim();
        JwtClaims claims = tokenProvider.parseToken(token);
        AuthContext.setCurrentUser(tokenProvider.toAuthenticatedUser(claims));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
