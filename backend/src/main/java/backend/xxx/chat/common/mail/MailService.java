package backend.xxx.chat.common.mail;

public interface MailService {

    void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl);

    void sendEmailVerificationEmail(String recipientEmail, String recipientName, String verificationUrl);

    void sendLoginAlertEmail(String recipientEmail, String recipientName, String ipAddress, String userAgent, String loggedInAt);

    void sendPasswordChangedEmail(String recipientEmail, String recipientName, String changedAt);
}

