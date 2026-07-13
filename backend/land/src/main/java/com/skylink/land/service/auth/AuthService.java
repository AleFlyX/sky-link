package com.skylink.land.service.auth;

import com.skylink.land.dto.auth.AuthDto;

public interface AuthService {

    AuthDto.RegisterResponse register(AuthDto.RegisterRequest request);

    AuthDto.TokenResponse login(AuthDto.LoginRequest request);

    void logout();
}
