package backend.xxx.chat.common.ratelimit;

import backend.xxx.chat.common.exception.LimitExceedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitProvider {

    private final RedisTemplate<String, Object> redisTemplate;

    public void rateLimit(String ipClient, String action, int maxReq, Duration windowDuration) {
        String key = ipClient + ":" + action;

        long now = System.currentTimeMillis();
        long window = windowDuration.toMillis();
        long windowStart = now - window;
        ZSetOperations<String, Object> zSetOperations = this.redisTemplate.opsForZSet();

        zSetOperations.removeRangeByScore(key, 0, windowStart);
        Long count = zSetOperations.zCard(key);

        if (count != null && count >= maxReq) {
            throw new LimitExceedException();
        }

        String member = ipClient + ":" + UUID.randomUUID();
        zSetOperations.add(key, member, now);
        this.redisTemplate.expire(key, windowDuration);
    }
}
