package backend.xxx.chat.auth.service;

import java.time.Duration;

import backend.xxx.chat.auth.exception.InvalidPasswordResetTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PasswordResetTokenService tokenService;

    @Test
    void issueStoresOnlyHashedTokenWithTtl() {
        Duration ttl = Duration.ofMinutes(15);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = tokenService.issue(10L, ttl);

        assertThat(token).isNotBlank();
        assertThat(token).doesNotContain("+").doesNotContain("/").doesNotContain("=");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq(10L), eq(ttl));
        assertThat(keyCaptor.getValue())
                .startsWith("password-reset:")
                .doesNotContain(token);
    }

    @Test
    void consumeAtomicallyDeletesTokenAndReturnsUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(10L);

        Long userId = tokenService.consume("reset-token");

        assertThat(userId).isEqualTo(10L);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).getAndDelete(keyCaptor.capture());
        assertThat(keyCaptor.getValue())
                .startsWith("password-reset:")
                .doesNotContain("reset-token");
    }

    @Test
    void consumeRejectsMissingOrExpiredToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThatThrownBy(() -> tokenService.consume("expired-token"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void issueRejectsNonPositiveTtl() {
        assertThatThrownBy(() -> tokenService.issue(10L, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.password.reset.ttl.positive");
    }
}

