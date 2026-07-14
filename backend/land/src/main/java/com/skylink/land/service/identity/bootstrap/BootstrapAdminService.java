package com.skylink.land.service.identity.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BootstrapAdminService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final BootstrapAdminProperties properties;

    public BootstrapAdminService(
        UserMapper userMapper,
        UserRoleMapper userRoleMapper,
        PasswordEncoder passwordEncoder,
        BootstrapAdminProperties properties
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public void bootstrap(Role superAdminRole) {
        properties.validate();
        if (!properties.isEnabled()) {
            return;
        }

        String username = properties.getUsername().trim();
        User user = userMapper.selectByUsernameIncludingDeleted(username);
        if (user == null) {
            user = createAdmin(username);
        } else {
            validateExistingUser(user, username);
        }
        bindSuperAdminRole(user, superAdminRole);
    }

    private User createAdmin(String username) {
        String email = properties.getEmail().trim();
        String phone = properties.getPhone().trim();
        Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        Long phoneCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if ((emailCount != null && emailCount > 0) || (phoneCount != null && phoneCount > 0)) {
            throw new IllegalStateException("Bootstrap administrator email or phone is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(properties.getPassword()));
        user.setNickname(StringUtils.hasText(properties.getNickname()) ? properties.getNickname().trim() : username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("Created bootstrap administrator account: {}", username);
        return user;
    }

    private void validateExistingUser(User user, String username) {
        if (Integer.valueOf(1).equals(user.getDeleted())) {
            throw new IllegalStateException("Bootstrap administrator username belongs to a deleted user: " + username);
        }
        if (!properties.getEmail().trim().equals(user.getEmail())
            || !properties.getPhone().trim().equals(user.getPhone())) {
            throw new IllegalStateException(
                "Bootstrap administrator username belongs to a user with different contact information: " + username
            );
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalStateException("Bootstrap administrator exists but is disabled: " + username);
        }
    }

    private void bindSuperAdminRole(User user, Role superAdminRole) {
        Long relationCount = userRoleMapper.selectCount(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getUserId())
                .eq(UserRole::getRoleId, superAdminRole.getRoleId())
        );
        if (relationCount != null && relationCount > 0) {
            return;
        }
        UserRole relation = new UserRole();
        relation.setUserId(user.getUserId());
        relation.setRoleId(superAdminRole.getRoleId());
        userRoleMapper.insert(relation);
    }
}
