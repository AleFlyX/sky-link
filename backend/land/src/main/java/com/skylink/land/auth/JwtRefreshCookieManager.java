package com.skylink.land.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtRefreshCookieManager {

    private final JwtProperties properties;

    public JwtRefreshCookieManager(JwtProperties properties) {
        this.properties = properties;
    }

    public void addRefreshToken(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie(refreshToken)
            .maxAge(properties.getRefreshTtl())
            .build()
            .toString());
    }

    public void clearRefreshToken(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie("")
            .maxAge(0)
            .build()
            .toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        JwtProperties.RefreshCookie cookie = properties.getRefreshCookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookie.getName(), value)
            .httpOnly(cookie.isHttpOnly())
            .secure(cookie.isSecure())
            .sameSite(cookie.getSameSite())
            .path(cookie.getPath());

        if (StringUtils.hasText(cookie.getDomain())) {
            builder.domain(cookie.getDomain());
        }

        return builder;
    }
}
