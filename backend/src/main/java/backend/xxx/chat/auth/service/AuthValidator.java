package backend.xxx.chat.auth.service;

import java.time.Duration;
import java.util.Locale;

import backend.xxx.chat.auth.exception.PasswordConfirmationMismatchException;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator {

    public void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new PasswordConfirmationMismatchException();
        }
    }

    public void validateRedisValue(String keyName, Object value, Duration timeToLive) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("redis.key.blank");
        }

        if (value == null) {
            throw new IllegalArgumentException("redis.value.null");
        }

        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("redis.ttl.positive");
        }
    }

    public String normalizeUsername(String username) {
        return username.trim();
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
