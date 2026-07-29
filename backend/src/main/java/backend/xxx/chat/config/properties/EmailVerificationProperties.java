package backend.xxx.chat.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.email-verification")
public record EmailVerificationProperties(
        Duration tokenTtl,
        String frontendUrl
) {
}
