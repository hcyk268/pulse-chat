package backend.xxx.chat.common.mail;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import backend.xxx.chat.common.exception.ServiceUnavailableException;
import backend.xxx.chat.config.properties.SendGridProperties;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class SendGridMailService implements MailService {

    private static final String SEND_MAIL_ENDPOINT = "mail/send";
    private static final Locale EMAIL_LOCALE = Locale.forLanguageTag("vi");

    private final SendGrid sendGrid;
    private final SendGridProperties properties;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl) {
        Email from = new Email(properties.fromEmail(), properties.fromName());
        Email to = new Email(recipientEmail, recipientName);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(properties.passwordResetSubject());
        mail.addPersonalization(personalization(to));
        mail.addContent(new Content("text/plain", passwordResetPlainText(recipientName, resetUrl)));
        mail.addContent(new Content(
                "text/html",
                renderTemplate(
                        "email/password-reset",
                        Map.of(
                                "recipientName", recipientName,
                                "actionUrl", resetUrl
                        )
                )
        ));

        send(mail);
    }

    @Override
    public void sendEmailVerificationEmail(String recipientEmail, String recipientName, String verificationUrl) {
        sendMail(
                recipientEmail,
                recipientName,
                properties.emailVerificationSubject(),
                "Hello %s,%n%nVerify your ChatApp email address:%n%s%n%nThis link expires soon."
                        .formatted(recipientName, verificationUrl),
                renderTemplate(
                        "email/email-verification",
                        Map.of(
                                "recipientName", recipientName,
                                "actionUrl", verificationUrl
                        )
                )
        );
    }

    @Override
    public void sendLoginAlertEmail(
            String recipientEmail,
            String recipientName,
            String ipAddress,
            String userAgent,
            String loggedInAt
    ) {
        String details = "Time: %s%nIP: %s%nDevice: %s".formatted(loggedInAt, ipAddress, userAgent);
        sendMail(
                recipientEmail,
                recipientName,
                properties.loginAlertSubject(),
                "Hello %s,%n%nA new login to your ChatApp account was detected.%n%s%n%nIf this was not you, reset your password immediately."
                        .formatted(recipientName, details),
                renderTemplate(
                        "email/login-alert",
                        Map.of(
                                "recipientName", recipientName,
                                "loggedInAt", loggedInAt,
                                "ipAddress", ipAddress,
                                "userAgent", userAgent
                        )
                )
        );
    }

    @Override
    public void sendPasswordChangedEmail(String recipientEmail, String recipientName, String changedAt) {
        sendMail(
                recipientEmail,
                recipientName,
                properties.passwordChangedSubject(),
                "Hello %s,%n%nYour ChatApp password was changed at %s.%nIf this was not you, contact support immediately."
                        .formatted(recipientName, changedAt),
                renderTemplate(
                        "email/password-changed",
                        Map.of(
                                "recipientName", recipientName,
                                "changedAt", changedAt
                        )
                )
        );
    }

    private void sendMail(
            String recipientEmail,
            String recipientName,
            String subject,
            String plainText,
            String html
    ) {
        Mail mail = new Mail();
        mail.setFrom(new Email(properties.fromEmail(), properties.fromName()));
        mail.setSubject(subject);
        mail.addPersonalization(personalization(new Email(recipientEmail, recipientName)));
        mail.addContent(new Content("text/plain", plainText));
        mail.addContent(new Content("text/html", html));
        send(mail);
    }

    private void send(Mail mail) {
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint(SEND_MAIL_ENDPOINT);
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new ServiceUnavailableException("mail.sendgrid.delivery.failed");
            }
        } catch (IOException ex) {
            throw new ServiceUnavailableException("mail.sendgrid.delivery.failed");
        }
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        Context context = new Context(EMAIL_LOCALE);
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }

    private Personalization personalization(Email recipient) {
        Personalization personalization = new Personalization();
        personalization.addTo(recipient);
        return personalization;
    }

    private String passwordResetPlainText(String recipientName, String resetUrl) {
        return """
                Hello %s,

                We received a request to reset your ChatApp password.
                Open this link to choose a new password:
                %s

                If you did not request this, you can safely ignore this email.
                """.formatted(recipientName, resetUrl);
    }
}
