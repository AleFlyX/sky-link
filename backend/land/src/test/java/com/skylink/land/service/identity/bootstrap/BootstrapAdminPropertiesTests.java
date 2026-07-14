package com.skylink.land.service.identity.bootstrap;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BootstrapAdminPropertiesTests {

    @Test
    void disabledBootstrapDoesNotRequireCredentials() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        assertThatNoException().isThrownBy(properties::validate);
    }

    @Test
    void enabledBootstrapRequiresAllCredentials() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setEnabled(true);
        assertThatThrownBy(properties::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SKYLINK_BOOTSTRAP_ADMIN_USERNAME");
    }

    @Test
    void enabledBootstrapRejectsWeakPassword() {
        BootstrapAdminProperties properties = validProperties();
        properties.setPassword("password");
        assertThatThrownBy(properties::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 12 characters");
    }

    @Test
    void enabledBootstrapAcceptsStrongCredentials() {
        assertThatNoException().isThrownBy(validProperties()::validate);
    }

    private BootstrapAdminProperties validProperties() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("bootstrap1234");
        properties.setEmail("admin@example.com");
        properties.setPhone("13800000000");
        return properties;
    }
}
