package com.skylink.land.service.identity.bootstrap;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "skylink.bootstrap.admin")
public class BootstrapAdminProperties {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private boolean enabled;

    private String username;

    @ToString.Exclude
    private String password;

    private String nickname;

    private String email;

    private String phone;

    public void validate() {
        if (!enabled) {
            return;
        }
        requireText("SKYLINK_BOOTSTRAP_ADMIN_USERNAME", username);
        requireText("SKYLINK_BOOTSTRAP_ADMIN_PASSWORD", password);
        requireText("SKYLINK_BOOTSTRAP_ADMIN_EMAIL", email);
        requireText("SKYLINK_BOOTSTRAP_ADMIN_PHONE", phone);
        if (password.length() < MIN_PASSWORD_LENGTH
            || password.chars().noneMatch(Character::isLetter)
            || password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalStateException(
                "SKYLINK_BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters and contain letters and numbers"
            );
        }
    }

    private void requireText(String environmentName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(environmentName + " must be configured when admin bootstrap is enabled");
        }
    }
}
