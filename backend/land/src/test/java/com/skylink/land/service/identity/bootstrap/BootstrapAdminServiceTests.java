package com.skylink.land.service.identity.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminServiceTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredAdminAndAssignsSuperAdminRole() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("bootstrap1234");
        properties.setEmail("admin@example.com");
        properties.setPhone("13800000000");
        when(passwordEncoder.encode("bootstrap1234")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, User.class).setUserId(101L);
            return 1;
        });
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        Role superAdminRole = new Role();
        superAdminRole.setRoleId(7L);
        BootstrapAdminService service = new BootstrapAdminService(
            userMapper,
            userRoleMapper,
            passwordEncoder,
            properties
        );
        service.bootstrap(superAdminRole);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        ArgumentCaptor<UserRole> relationCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(101L);
        assertThat(relationCaptor.getValue().getRoleId()).isEqualTo(7L);
    }
}
