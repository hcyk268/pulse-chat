package backend.xxx.chat.config;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.config.properties.EmailVerificationProperties;
import backend.xxx.chat.config.properties.PasswordResetProperties;
import backend.xxx.chat.config.properties.SendGridProperties;
import com.sendgrid.SendGrid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        EmailVerificationProperties.class,
        PasswordResetProperties.class,
        SendGridProperties.class
})
public class AuthConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.mail.sendgrid", name = "enabled", havingValue = "true")
    public SendGrid sendGrid(SendGridProperties properties) {
        requireNotBlank(properties.apiKey(), "mail.sendgrid.api-key.blank");
        requireNotBlank(properties.fromEmail(), "mail.sendgrid.from-email.blank");
        requireNotBlank(properties.fromName(), "mail.sendgrid.from-name.blank");
        requireNotBlank(properties.emailVerificationSubject(), "mail.sendgrid.email-verification-subject.blank");
        requireNotBlank(properties.loginAlertSubject(), "mail.sendgrid.login-alert-subject.blank");
        requireNotBlank(properties.passwordChangedSubject(), "mail.sendgrid.password-changed-subject.blank");
        requireNotBlank(properties.passwordResetSubject(), "mail.sendgrid.password-reset-subject.blank");
        return new SendGrid(properties.apiKey());
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}

