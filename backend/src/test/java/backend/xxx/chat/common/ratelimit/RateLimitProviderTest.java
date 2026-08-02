package backend.xxx.chat.common.ratelimit;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.common.exception.LimitExceedException;
import backend.xxx.chat.common.exception.RateLimitUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitProviderTest {

    private static final String KEY = "rate-limit:v1:login:203.0.113.10";

    @Test
    void buildsRedisKey() {
        RedisFixture redis = RedisFixture.create();
        when(redis.zSetOperations.zCard(KEY)).thenReturn(0L);
        when(redis.redisOperations.exec()).thenReturn(List.of(Boolean.TRUE, Boolean.TRUE));
        RateLimitProvider provider = new TestRateLimitProvider(redis.redisTemplate, 60_000L);

        assertThatCode(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .doesNotThrowAnyException();

        verify(redis.redisTemplate.opsForZSet()).removeRangeByScore(KEY, 0, 0);
        verify(redis.redisOperations).watch(KEY);
        verify(redis.zSetOperations).zCard(KEY);
        verify(redis.redisOperations).multi();
        verify(redis.zSetOperations).add(eq(KEY), anyString(), eq(60_000.0));
        verify(redis.redisOperations).expire(KEY, Duration.ofMinutes(1));
        verify(redis.redisOperations).exec();
    }

    @Test
    void roundsRetrySeconds() {
        RedisFixture redis = RedisFixture.create();
        when(redis.zSetOperations.zCard(KEY)).thenReturn(5L);
        when(redis.zSetOperations.rangeWithScores(KEY, 0, 0))
                .thenReturn(Set.of(new DefaultTypedTuple<>((Object) "oldest", 1_501.0)));
        RateLimitProvider provider = new TestRateLimitProvider(redis.redisTemplate, 60_000L);

        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOfSatisfying(LimitExceedException.class,
                        exception -> assertThat(exception.getRetryAfterSeconds()).isEqualTo(2L));

        verify(redis.redisOperations).unwatch();
    }

    @Test
    void failsWhenRedisUnavailable() {
        RedisFixture unavailable = RedisFixture.create();
        when(unavailable.redisTemplate.opsForZSet().removeRangeByScore(KEY, 0, 0))
                .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

        assertThatThrownBy(() -> new TestRateLimitProvider(unavailable.redisTemplate, 60_000L)
                .rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitUnavailableException.class);

        RedisFixture conflicted = RedisFixture.create();
        when(conflicted.redisTemplate.execute(any(SessionCallback.class))).thenReturn(null);

        assertThatThrownBy(() -> new TestRateLimitProvider(conflicted.redisTemplate, 60_000L)
                .rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitUnavailableException.class);
    }

    @Test
    void rejectsInvalidPolicy() {
        RedisFixture redis = RedisFixture.create();
        RateLimitProvider provider = new TestRateLimitProvider(redis.redisTemplate, 60_000L);

        assertThatThrownBy(() -> provider.rateLimit("", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class TestRateLimitProvider extends RateLimitProvider {

        private final long nowMillis;

        private TestRateLimitProvider(RedisTemplate<String, Object> redisTemplate, long nowMillis) {
            super(redisTemplate);
            this.nowMillis = nowMillis;
        }

        @Override
        protected long nowMillis() {
            return nowMillis;
        }
    }

    private record RedisFixture(
            RedisTemplate<String, Object> redisTemplate,
            RedisOperations<String, Object> redisOperations,
            ZSetOperations<String, Object> zSetOperations
    ) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static RedisFixture create() {
            RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
            RedisOperations<String, Object> redisOperations = mock(RedisOperations.class);
            ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.opsForZSet().removeRangeByScore(anyString(), eq(0.0), any(Double.class)))
                    .thenReturn(0L);
            when(redisOperations.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
                SessionCallback callback = invocation.getArgument(0);
                return callback.execute(redisOperations);
            });

            return new RedisFixture(redisTemplate, redisOperations, zSetOperations);
        }
    }
}
