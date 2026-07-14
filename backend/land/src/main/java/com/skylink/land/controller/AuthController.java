package com.skylink.land.controller;

import com.skylink.land.auth.JwtRefreshCookieManager;
import com.skylink.land.auth.TokenPair;
import com.skylink.land.dto.auth.AuthDto;
import com.skylink.land.service.auth.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final JwtRefreshCookieManager refreshCookieManager;

    public AuthController(
        AuthService authService,
        JwtRefreshCookieManager refreshCookieManager
    ) {
        this.authService = authService;
        this.refreshCookieManager = refreshCookieManager;
    }

    @PostMapping("/register")
    public AuthDto.RegisterResponse register(@RequestBody AuthDto.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDto.TokenResponse login(@RequestBody AuthDto.LoginRequest request, HttpServletResponse response) {
        TokenPair tokenPair = authService.login(request);
        refreshCookieManager.addRefreshToken(response, tokenPair.getRefreshToken());
        return tokenPair.toResponse();
    }

    @PostMapping("/refresh")
    public AuthDto.TokenResponse refresh(
        @CookieValue(name = "${skylink.jwt.refresh-cookie.name}", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.refresh(refreshToken);
        refreshCookieManager.addRefreshToken(response, tokenPair.getRefreshToken());
        return tokenPair.toResponse();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        authService.logout();
        refreshCookieManager.clearRefreshToken(response);
    }
}
