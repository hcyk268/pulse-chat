package backend.xxx.chat.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.password-reset")
public record PasswordResetProperties(
        Duration tokenTtl,
        String frontendUrl
) {
}

