package backend.xxx.chat.auth.mail;

import backend.xxx.chat.auth.service.EmailVerificationTokenService;
import backend.xxx.chat.auth.service.PasswordResetTokenService;
import backend.xxx.chat.common.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMailAsyncService {

    private final MailService mailService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    @Async
    public void sendPasswordReset(
            String recipientEmail,
            String recipientName,
            String resetUrl,
            String token
    ) {
        try {
            mailService.sendPasswordResetEmail(recipientEmail, recipientName, resetUrl);
        } catch (RuntimeException ex) {
            passwordResetTokenService.invalidate(token);
            log.error("Could not send password reset email to {}", recipientEmail, ex);
        }
    }

    @Async
    public void sendEmailVerification(
            String recipientEmail,
            String recipientName,
            String verificationUrl,
            String token
    ) {
        try {
            mailService.sendEmailVerificationEmail(recipientEmail, recipientName, verificationUrl);
        } catch (RuntimeException ex) {
            emailVerificationTokenService.invalidate(token);
            log.error("Could not send email verification to {}", recipientEmail, ex);
        }
    }

    @Async
    public void sendLoginAlert(
            String recipientEmail,
            String recipientName,
            String ipAddress,
            String userAgent,
            String loggedInAt
    ) {
        try {
            mailService.sendLoginAlertEmail(
                    recipientEmail,
                    recipientName,
                    ipAddress,
                    userAgent,
                    loggedInAt
            );
        } catch (RuntimeException ex) {
            log.error("Could not send login alert email to {}", recipientEmail, ex);
        }
    }

    @Async
    public void sendPasswordChanged(
            String recipientEmail,
            String recipientName,
            String changedAt
    ) {
        try {
            mailService.sendPasswordChangedEmail(recipientEmail, recipientName, changedAt);
        } catch (RuntimeException ex) {
            log.error("Could not send password changed email to {}", recipientEmail, ex);
        }
    }
}
