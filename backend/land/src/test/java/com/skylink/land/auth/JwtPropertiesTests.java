package com.skylink.land.auth;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTests {

    @Test
    void rejectsMissingSecret() {
        JwtProperties properties = new JwtProperties();

        assertThatThrownBy(properties::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsInsecureExampleSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("change-me-to-a-long-random-secret-at-least-32-bytes");

        assertThatThrownBy(properties::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("example value");
    }

    @Test
    void acceptsStrongSecretAndPositiveTtl() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setTtl(Duration.ofHours(2));
        properties.setRefreshTtl(Duration.ofDays(7));

        assertThatNoException().isThrownBy(properties::afterPropertiesSet);
    }

    @Test
    void rejectsRefreshTtlThatIsNotLongerThanAccessTtl() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setTtl(Duration.ofHours(2));
        properties.setRefreshTtl(Duration.ofHours(2));

        assertThatThrownBy(properties::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("longer than access token");
    }

    @Test
    void rejectsSameSiteNoneWithoutSecureCookie() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.getRefreshCookie().setSameSite("None");

        assertThatThrownBy(properties::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SameSite=None");
    }
}
