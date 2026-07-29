package backend.xxx.chat.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import backend.xxx.chat.auth.exception.InvalidPasswordResetTokenException;
import backend.xxx.chat.auth.exception.RedisUnavailable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final String KEY_PREFIX = "password-reset:";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;

    public String issue(Long userId, Duration timeToLive) {
        if (userId == null) {
            throw new IllegalArgumentException("user.id.required");
        }
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("auth.password.reset.ttl.positive");
        }

        String token = generateToken();
        try {
            redisTemplate.opsForValue().set(key(token), userId, timeToLive);
            return token;
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    public Long consume(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        try {
            Object value = redisTemplate.opsForValue().getAndDelete(key(token));
            if (!(value instanceof Number userId)) {
                throw new InvalidPasswordResetTokenException();
            }
            return userId.longValue();
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    public void invalidate(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            redisTemplate.delete(key(token));
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String key(String token) {
        return KEY_PREFIX + hash(token);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("auth.password.reset.hash.failed", ex);
        }
    }
}
