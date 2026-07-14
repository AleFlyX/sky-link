package com.skylink.land.service.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.skylink.land.auth.JwtProperties;
import com.skylink.land.auth.JwtTokenProvider;
import com.skylink.land.dto.auth.AuthDto;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.auth.impl.AuthServiceImpl;
import com.skylink.land.service.identity.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void registrationFailsWhenDefaultRoleIsMissing() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, User.class).setUserId(101L);
            return 1;
        });

        AuthServiceImpl service = new AuthServiceImpl(
            userMapper,
            roleMapper,
            userRoleMapper,
            userService,
            passwordEncoder,
            jwtTokenProvider,
            new JwtProperties()
        );
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("new-user");
        request.setPassword("password123");
        request.setEmail("new-user@example.com");
        request.setPhone("13800000000");

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR)
            )
            .hasMessageContaining("default role");
    }
}
