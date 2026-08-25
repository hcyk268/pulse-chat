package backend.xxx.chat.ai.memory;

import backend.xxx.chat.ai.client.AiChatMessage;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.config.properties.AIProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatMemoryService {

    private static final String KEY_PREFIX = "ai:memory:user:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final AIProperties properties;
    private final ObjectMapper objectMapper;

    public List<AiChatMessage> load(AiExecutionContext context, Long conversationId) {
        if (!enabled(context, conversationId)) {
            return List.of();
        }
        try {
            List<Object> values = redisTemplate.opsForList().range(key(context, conversationId), 0, -1);
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return values.stream()
                    .map(this::toTurn)
                    .filter(turn -> turn != null && StringUtils.hasText(turn.content()))
                    .map(turn -> new AiChatMessage(turn.role(), turn.content()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("AI chat memory load failed conversationId={} type={}", conversationId, ex.getClass().getSimpleName());
            return List.of();
        }
    }

    public void append(AiExecutionContext context, Long conversationId, String userQuestion, String assistantAnswer) {
        if (!enabled(context, conversationId)) {
            return;
        }
        try {
            String key = key(context, conversationId);
            if (StringUtils.hasText(userQuestion)) {
                redisTemplate.opsForList().rightPush(key, new AiMemoryTurn("user", userQuestion.trim(), Instant.now()));
            }
            if (StringUtils.hasText(assistantAnswer)) {
                redisTemplate.opsForList().rightPush(key, new AiMemoryTurn("assistant", assistantAnswer.trim(), Instant.now()));
            }
            redisTemplate.opsForList().trim(key, -properties.getMemory().getMaxMessages(), -1);
            redisTemplate.expire(key, properties.getMemory().getTtl());
        } catch (RuntimeException ex) {
            log.warn("AI chat memory append failed conversationId={} type={}", conversationId, ex.getClass().getSimpleName());
        }
    }

    private AiMemoryTurn toTurn(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof AiMemoryTurn turn) {
            return turn;
        }
        return objectMapper.convertValue(value, AiMemoryTurn.class);
    }
    private boolean enabled(AiExecutionContext context, Long conversationId) {
        return properties.getMemory().isEnabled()
                && context != null
                && context.currentUserId() != null
                && conversationId != null;
    }

    private String key(AiExecutionContext context, Long conversationId) {
        return KEY_PREFIX + context.currentUserId() + ":conversation:" + conversationId;
    }

    public record AiMemoryTurn(String role, String content, Instant createdAt) {
    }
}
