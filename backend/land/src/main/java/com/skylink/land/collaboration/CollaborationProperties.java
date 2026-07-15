package com.skylink.land.collaboration;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("skylink.collaboration")
public class CollaborationProperties {
    private String issuer = "sky-link";
    private String audience = "sky-link-collaboration";
    private String ticketSecret;
    private Duration ticketTtl = Duration.ofSeconds(60);
    private String websocketUrl = "ws://127.0.0.1:8180";
    private String serviceToken;
}
