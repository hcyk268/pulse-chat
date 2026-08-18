package backend.xxx.chat.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail.sendgrid")
public record SendGridProperties(
        String apiKey,
        String fromEmail,
        String fromName,
        String emailVerificationSubject,
        String loginAlertSubject,
        String passwordChangedSubject,
        String passwordResetSubject
) {
}

