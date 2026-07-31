package backend.xxx.chat.common.ratelimit;

import java.time.Duration;
import java.util.List;

import backend.xxx.chat.common.exception.LimitExceedException;
import backend.xxx.chat.common.exception.RateLimitUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitProviderTest {

    @Test
    void buildsRedisKey() {
        StubRedisTemplate redis = new StubRedisTemplate(0L);
        RateLimitProvider provider = new RateLimitProvider(redis);

        assertThatCode(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .doesNotThrowAnyException();

        assertThat(redis.keys).containsExactly("rate-limit:v1:login:203.0.113.10");
        assertThat(redis.arguments).containsExactly("60000", "5", redis.arguments[2]);
    }

    @Test
    void roundsRetrySeconds() {
        RateLimitProvider provider = new RateLimitProvider(new StubRedisTemplate(1_501L));

        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOfSatisfying(LimitExceedException.class,
                        exception -> assertThat(exception.getRetryAfterSeconds()).isEqualTo(2L));
    }

    @Test
    void failsWhenRedisUnavailable() {
        StubRedisTemplate unavailable = new StubRedisTemplate(0L);
        unavailable.failure = new DataAccessResourceFailureException("redis unavailable");

        assertThatThrownBy(() -> new RateLimitProvider(unavailable)
                .rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitUnavailableException.class);

        assertThatThrownBy(() -> new RateLimitProvider(new StubRedisTemplate(null))
                .rateLimit("203.0.113.10", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitUnavailableException.class);
    }

    @Test
    void rejectsInvalidPolicy() {
        RateLimitProvider provider = new RateLimitProvider(new StubRedisTemplate(0L));

        assertThatThrownBy(() -> provider.rateLimit("", "login", 5, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.rateLimit("203.0.113.10", "login", 5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class StubRedisTemplate extends StringRedisTemplate {

        private final Long result;
        private RuntimeException failure;
        private List<String> keys;
        private Object[] arguments;

        private StubRedisTemplate(Long result) {
            this.result = result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            if (failure != null) {
                throw failure;
            }
            this.keys = keys;
            this.arguments = args;
            return (T) result;
        }
    }
}
