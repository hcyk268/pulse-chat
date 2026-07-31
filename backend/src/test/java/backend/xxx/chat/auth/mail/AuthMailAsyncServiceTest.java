package backend.xxx.chat.auth.mail;

import backend.xxx.chat.auth.service.EmailVerificationTokenService;
import backend.xxx.chat.auth.service.PasswordResetTokenService;
import backend.xxx.chat.common.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthMailAsyncServiceTest {

    @Mock
    private MailService mailService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    private AuthMailAsyncService asyncService;

    @BeforeEach
    void setUp() {
        asyncService = new AuthMailAsyncService(
                mailService,
                passwordResetTokenService,
                emailVerificationTokenService
        );
    }

    @Test
    void invalidatesResetToken() {
        doThrow(new IllegalStateException("mail unavailable"))
                .when(mailService)
                .sendPasswordResetEmail("alice@example.com", "Alice", "https://app.example/reset");

        asyncService.sendPasswordReset(
                "alice@example.com",
                "Alice",
                "https://app.example/reset",
                "reset-token"
        );

        verify(passwordResetTokenService).invalidate("reset-token");
    }

    @Test
    void invalidatesVerificationToken() {
        doThrow(new IllegalStateException("mail unavailable"))
                .when(mailService)
                .sendEmailVerificationEmail("alice@example.com", "Alice", "https://app.example/verify");

        asyncService.sendEmailVerification(
                "alice@example.com",
                "Alice",
                "https://app.example/verify",
                "verification-token"
        );

        verify(emailVerificationTokenService).invalidate("verification-token");
    }
}
