package backend.xxx.chat.common.ratelimit;

import backend.xxx.chat.common.exception.LimitExceedException;
import backend.xxx.chat.common.exception.RateLimitUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitProvider {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local windowMillis = tonumber(ARGV[1])
            local maxRequests = tonumber(ARGV[2])
            local member = ARGV[3]
            local redisTime = redis.call('TIME')
            local now = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)

            redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMillis)
            if redis.call('ZCARD', key) >= maxRequests then
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                if oldest[2] then
                    return math.max(1, math.floor(tonumber(oldest[2]) + windowMillis - now))
                end
                return windowMillis
            end

            redis.call('ZADD', key, now, member)
            redis.call('PEXPIRE', key, windowMillis)
            return 0
            """,
            Long.class
    );
    private final StringRedisTemplate stringRedisTemplate;

    public void rateLimit(String ipClient, String action, int maxReq, Duration windowDuration) {
        validate(ipClient, action, maxReq, windowDuration);
        String key = "rate-limit:v1:" + action + ":" + ipClient;
        long windowMillis = windowDuration.toMillis();
        String member = UUID.randomUUID().toString();

        final Long retryAfterMillis;
        try {
            retryAfterMillis = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    Long.toString(windowMillis),
                    Integer.toString(maxReq),
                    member
            );
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

    private long toRetryAfterSeconds(long retryAfterMillis) {
        return Math.max(1L, (retryAfterMillis + 999L) / 1_000L);
    }
}
