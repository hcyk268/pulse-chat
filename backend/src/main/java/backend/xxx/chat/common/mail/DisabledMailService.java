package backend.xxx.chat.common.mail;

import backend.xxx.chat.common.exception.ServiceUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "app.mail.sendgrid",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledMailService implements MailService {

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl) {
        throw new ServiceUnavailableException("mail.sendgrid.not.configured");
    }

    @Override
    public void sendEmailVerificationEmail(String recipientEmail, String recipientName, String verificationUrl) {
        throw new ServiceUnavailableException("mail.sendgrid.not.configured");
    }

    @Override
    public void sendLoginAlertEmail(String recipientEmail, String recipientName, String ipAddress, String userAgent, String loggedInAt) {
        throw new ServiceUnavailableException("mail.sendgrid.not.configured");
    }

    @Override
    public void sendPasswordChangedEmail(String recipientEmail, String recipientName, String changedAt) {
        throw new ServiceUnavailableException("mail.sendgrid.not.configured");
    }
}

