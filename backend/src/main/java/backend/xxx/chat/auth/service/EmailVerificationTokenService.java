package backend.xxx.chat.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import backend.xxx.chat.auth.exception.InvalidEmailVerificationTokenException;
import backend.xxx.chat.auth.exception.RedisUnavailable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private static final String TOKEN_KEY_PREFIX = "email-verification:";
    private static final String USER_KEY_PREFIX = "email-verification-user:";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;

    public String createVerToken(Long userId, Duration timeToLive) {
        validate(userId, timeToLive);

        String token = generateToken();
        String tokenHash = hash(token);
        String userKey = userKey(userId);

        try {
            Object oldHash = redisTemplate.opsForValue().get(userKey);
            if (oldHash instanceof String previousHash) {
                redisTemplate.delete(tokenKey(previousHash));
            }

            redisTemplate.opsForValue().set(tokenKey(tokenHash), userId, timeToLive);
            redisTemplate.opsForValue().set(userKey, tokenHash, timeToLive);
            return token;
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    public Long consume(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidEmailVerificationTokenException();
        }

        try {
            Object value = redisTemplate.opsForValue().getAndDelete(tokenKey(hash(token)));
            if (!(value instanceof Number userId)) {
                throw new InvalidEmailVerificationTokenException();
            }

            redisTemplate.delete(userKey(userId.longValue()));
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
            Object value = redisTemplate.opsForValue().getAndDelete(tokenKey(hash(token)));
            if (value instanceof Number userId) {
                redisTemplate.delete(userKey(userId.longValue()));
            }
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private void validate(Long userId, Duration timeToLive) {
        if (userId == null) {
            throw new IllegalArgumentException("user.id.required");
        }
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("auth.email.verification.ttl.positive");
        }
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("auth.email.verification.hash.failed", ex);
        }
    }
}
