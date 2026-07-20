package backend.xxx.chat.auth.service;

import java.time.Duration;

import backend.xxx.chat.auth.exception.PasswordConfirmationMismatchException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValidatorTest {

    private final AuthValidator authValidator = new AuthValidator();

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(authValidator.normalizeEmail(" ALICE@EXAMPLE.COM "))
                .isEqualTo("alice@example.com");
    }

    @Test
    void validatePasswordConfirmationRejectsMismatch() {
        assertThatThrownBy(() -> authValidator.validatePasswordConfirmation("Password123!", "Different123!"))
                .isInstanceOf(PasswordConfirmationMismatchException.class);
    }

    @Test
    void validateRedisValueRequiresPositiveTtl() {
        assertThatThrownBy(() -> authValidator.validateRedisValue("refresh:key", new Object(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redis.ttl.positive");
    }
}
