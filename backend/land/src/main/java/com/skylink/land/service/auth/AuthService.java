package com.skylink.land.service.auth;

import com.skylink.land.auth.TokenPair;
import com.skylink.land.dto.auth.AuthDto;

public interface AuthService {

    AuthDto.RegisterResponse register(AuthDto.RegisterRequest request);

    TokenPair login(AuthDto.LoginRequest request);

    TokenPair refresh(String refreshToken);

    void logout();
}
