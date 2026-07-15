package com.skylink.land.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skylink.land.auth.JwtClaims;
import com.skylink.land.auth.JwtProperties;
import com.skylink.land.auth.JwtTokenProvider;
import com.skylink.land.auth.TokenPair;
import com.skylink.land.dto.auth.AuthDto;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.auth.AuthService;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.service.identity.bootstrap.SecurityBootstrapCatalog;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtProperties jwtProperties;

    public AuthServiceImpl(
        UserMapper userMapper,
        RoleMapper roleMapper,
        UserRoleMapper userRoleMapper,
        UserService userService,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        JwtProperties jwtProperties
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthDto.RegisterResponse register(AuthDto.RegisterRequest request) {
        validateRegisterRequest(request);
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        ensureUniqueUser(username, email, phone, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);

        bindDefaultRole(user.getUserId());
        User savedUser = userMapper.selectById(user.getUserId());

        return AuthDto.RegisterResponse.builder()
            .userId(savedUser.getUserId())
            .username(savedUser.getUsername())
            .nickname(savedUser.getNickname())
            .email(savedUser.getEmail())
            .phone(savedUser.getPhone())
            .createTime(savedUser.getCreateTime())
            .build();
    }

    @Override
    public TokenPair login(AuthDto.LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getAccount()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "account and password are required");
        }

        User user = findByAccount(request.getAccount().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid account or password");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "user is disabled");
        }

        return issueTokens(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        JwtClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        User user = userMapper.selectById(claims.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid refresh token");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "user is disabled");
        }

        return issueTokens(user);
    }

    private TokenPair issueTokens(User user) {
        List<String> roles = userService.listRoleCodes(user.getUserId());
        List<String> permissions = userService.listPermissionCodes(user.getUserId());
        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getUsername(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), user.getUsername(), roles);

        return TokenPair.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtProperties.getTtl().toSeconds())
            .userInfo(AuthDto.LoginUserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .departmentId(user.getDepartmentId())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .build())
            .build();
    }

    @Override
    public void logout() {
    }

    private void validateRegisterRequest(AuthDto.RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())
            || !StringUtils.hasText(request.getPassword())
            || !StringUtils.hasText(request.getEmail())
            || !StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username, password, email and phone are required");
        }
        if (!isValidPassword(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must be at least 8 characters and contain letters and numbers");
        }
    }

    private void ensureUniqueUser(String username, String email, String phone, Long excludeUserId) {
        checkUnique("username", username, excludeUserId);
        checkUnique("email", email, excludeUserId);
        checkUnique("phone", phone, excludeUserId);
    }

    private void checkUnique(String field, String value, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        switch (field) {
            case "username" -> wrapper.eq(User::getUsername, value);
            case "email" -> wrapper.eq(User::getEmail, value);
            case "phone" -> wrapper.eq(User::getPhone, value);
            default -> throw new IllegalArgumentException("unsupported field: " + field);
        }
        wrapper.ne(excludeUserId != null, User::getUserId, excludeUserId);
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, field + " already exists");
        }
    }

    private User findByAccount(String account) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, account).last("limit 1")
        );
        if (user != null) {
            return user;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, account).last("limit 1")
        );
    }

    private void bindDefaultRole(Long userId) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, SecurityBootstrapCatalog.ROLE_USER).last("limit 1")
        );
        if (role == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "default role is not initialized");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());
        userRoleMapper.insert(userRole);
    }

    private boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}
