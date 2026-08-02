package backend.xxx.chat.common.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import backend.xxx.chat.common.exception.LimitExceedException;
import backend.xxx.chat.common.exception.RateLimitUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitProvider {

    private static final int MAX_TRANSACTION_RETRIES = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    public void rateLimit(String ipClient, String action, int maxReq, Duration windowDuration) {
        validate(ipClient, action, maxReq, windowDuration);
        String key = "rate-limit:v1:" + action + ":" + ipClient;
        long windowMillis = windowDuration.toMillis();

        final Long retryAfterMillis;
        try {
            retryAfterMillis = checkLimitWithRedisTemplate(key, maxReq, windowDuration, windowMillis);
        } catch (DataAccessException ex) {
            throw new RateLimitUnavailableException();
        }

        if (retryAfterMillis == null) {
            throw new RateLimitUnavailableException();
        }
        if (retryAfterMillis > 0) {
            throw new LimitExceedException(toRetryAfterSeconds(retryAfterMillis));
        }
    }

    private Long checkLimitWithRedisTemplate(
            String key,
            int maxReq,
            Duration windowDuration,
            long windowMillis
    ) {
        for (int attempt = 0; attempt < MAX_TRANSACTION_RETRIES; attempt++) {
            long nowMillis = nowMillis();
            removeExpiredRequests(key, windowMillis, nowMillis);

            Long retryAfterMillis = redisTemplate.execute(new SessionCallback<>() {
                @Override
                public Long execute(RedisOperations operations) {
                    operations.watch(key);
                    ZSetOperations<String, Object> zSetOps = operations.opsForZSet();

                    Long requestCount = zSetOps.zCard(key);
                    if (requestCount == null) {
                        operations.unwatch();
                        throw new RateLimitUnavailableException();
                    }

                    if (requestCount >= maxReq) {
                        Long retryAfter = retryAfterMillis(key, zSetOps, windowMillis, nowMillis);
                        operations.unwatch();
                        return retryAfter;
                    }

                    operations.multi();
                    zSetOps.add(key, UUID.randomUUID().toString(), nowMillis);
                    operations.expire(key, windowDuration);
                    return operations.exec() == null ? null : 0L;
                }
            });

            if (retryAfterMillis != null) {
                return retryAfterMillis;
            }
        }
        return null;
    }

    private void removeExpiredRequests(String key, long windowMillis, long nowMillis) {
        Long removedCount = redisTemplate.opsForZSet()
                .removeRangeByScore(key, 0, nowMillis - windowMillis);
        if (removedCount == null) {
            throw new RateLimitUnavailableException();
        }
    }

    private Long retryAfterMillis(
            String key,
            ZSetOperations<String, Object> zSetOps,
            long windowMillis,
            long nowMillis
    ) {
        Set<ZSetOperations.TypedTuple<Object>> oldestEntries = zSetOps.rangeWithScores(key, 0, 0);
        if (oldestEntries == null) {
            throw new RateLimitUnavailableException();
        }
        return oldestEntries.stream()
                .findFirst()
                .map(ZSetOperations.TypedTuple::getScore)
                .map(Double::longValue)
                .map(oldestScore -> Math.max(1L, oldestScore + windowMillis - nowMillis))
                .orElse(windowMillis);
    }

    private void validate(String ipClient, String action, int maxReq, Duration windowDuration) {
        if (ipClient == null || ipClient.isBlank()) {
            throw new IllegalArgumentException("Rate-limit client IP must not be blank");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Rate-limit action must not be blank");
        }
        if (maxReq <= 0) {
            throw new IllegalArgumentException("Rate-limit max requests must be positive");
        }
        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }
    }

    protected long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    private long toRetryAfterSeconds(long retryAfterMillis) {
        return Math.max(1L, (retryAfterMillis + 999L) / 1_000L);
    }
}
