package backend.xxx.chat.ai.client;

import backend.xxx.chat.ai.config.AiDefaults;
import backend.xxx.chat.common.exception.ServiceUnavailableException;
import backend.xxx.chat.config.properties.AIProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiClient implements AiClient {

    private final ChatClient.Builder chatClientBuilder;
    private final AIProperties properties;

    private ChatClient chatClient;
    private volatile Semaphore requestSlots;

    @PostConstruct
    void initialize() {
        this.chatClient = chatClientBuilder.build();
        this.requestSlots = new Semaphore(AiDefaults.MAX_CONCURRENT_REQUESTS, true);
    }

    @Override
    public AiResponse complete(AiRequest request) {
        return complete(request, List.of(), Map.of());
    }

    @Override
    public AiResponse complete(AiRequest request, List<ToolCallback> toolCallbacks, Map<String, Object> toolContext) {
        Semaphore slots = requestSlots;
        if (slots == null || !slots.tryAcquire()) {
            throw new ServiceUnavailableException("ai.provider.busy");
        }

        try {
            return completeWithRetries(request, toolCallbacks == null ? List.of() : toolCallbacks,
                    toolContext == null ? Map.of() : toolContext);
        } finally {
            slots.release();
        }
    }


    @Override
    public <T> AiStructuredResponse<T> completeStructured(AiStructuredRequest<T> request) {
        Semaphore slots = requestSlots;
        if (slots == null || !slots.tryAcquire()) {
            throw new ServiceUnavailableException("ai.provider.busy");
        }

        try {
            return completeStructuredWithRetries(request);
        } finally {
            slots.release();
        }
    }

    private <T> AiStructuredResponse<T> completeStructuredWithRetries(AiStructuredRequest<T> request) {
        int maxAttempts = AiDefaults.MAX_RETRIES + 1;
        long deadlineNanos = System.nanoTime() + properties.getTimeout().toNanos();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doCompleteStructured(request);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.warn("AI structured provider request failed type={} attempt={}/{}", ex.getClass().getSimpleName(), attempt, maxAttempts);
            }
            if (attempt >= maxAttempts || deadlineReached(deadlineNanos)) {
                throw unavailable();
            }
            waitBeforeRetry(attempt, deadlineNanos);
        }
        throw unavailable();
    }

    private <T> AiStructuredResponse<T> doCompleteStructured(AiStructuredRequest<T> request) {
        OpenAiChatOptions options = buildStructuredOptions(request);
        ResponseEntity<ChatResponse, T> responseEntity = chatClient.prompt()
                .messages(toSpringMessages(request.messages()))
                .options(options)
                .call()
                .responseEntity(request.outputType());
        T entity = responseEntity.entity();
        if (entity == null) {
            throw new ServiceUnavailableException("ai.provider.empty.response");
        }
        ChatResponseMetadata metadata = responseEntity.response() == null ? null : responseEntity.response().getMetadata();
        return new AiStructuredResponse<>(entity, properties.getModel(), extractUsage(metadata));
    }
    private AiResponse completeWithRetries(
            AiRequest request,
            List<ToolCallback> toolCallbacks,
            Map<String, Object> toolContext
    ) {
        RuntimeException lastFailure = unavailable();
        int maxAttempts = AiDefaults.MAX_RETRIES + 1;
        long deadlineNanos = System.nanoTime() + properties.getTimeout().toNanos();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doComplete(request, toolCallbacks, toolContext);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                lastFailure = unavailable();
                log.warn("AI provider request failed type={} attempt={}/{}", ex.getClass().getSimpleName(), attempt, maxAttempts);
            }
            if (attempt >= maxAttempts || deadlineReached(deadlineNanos)) {
                throw lastFailure;
            }
            waitBeforeRetry(attempt, deadlineNanos);
        }
        throw lastFailure;
    }

    private AiResponse doComplete(
            AiRequest request,
            List<ToolCallback> toolCallbacks,
            Map<String, Object> toolContext
    ) {
        OpenAiChatOptions options = buildOptions(request);
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .messages(toSpringMessages(request.messages()))
                .options(options);
        if (!toolCallbacks.isEmpty()) {
            spec = spec.toolCallbacks(toolCallbacks).toolContext(toolContext);
        }

        ChatResponse chatResponse = spec.call().chatResponse();
        String content = chatResponse == null || chatResponse.getResult() == null
                ? ""
                : chatResponse.getResult().getOutput().getText();
        if (!StringUtils.hasText(content)) {
            throw new ServiceUnavailableException("ai.provider.empty.response");
        }

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        String model = metadata == null || !StringUtils.hasText(metadata.getModel())
                ? properties.getModel()
                : metadata.getModel();
        return new AiResponse(content, model, extractUsage(metadata));
    }


    private <T> OpenAiChatOptions buildStructuredOptions(AiStructuredRequest<T> request) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(request.temperature() == null ? properties.getTemperature() : request.temperature())
                .maxTokens(request.maxOutputTokens() == null ? properties.getMaxOutputTokens() : request.maxOutputTokens());
        if (request.nativeStructuredOutput()) {
            String schema = JsonSchemaGenerator.generateForType(request.outputType());
            builder.responseFormat(ResponseFormat.builder()
                    .type(ResponseFormat.Type.JSON_SCHEMA)
                    .jsonSchema(ResponseFormat.JsonSchema.builder()
                            .name(request.outputType().getSimpleName())
                            .schema(schema)
                            .strict(true)
                            .build())
                    .build());
            builder.outputSchema(schema);
        }
        return builder.build();
    }
    private OpenAiChatOptions buildOptions(AiRequest request) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(request.temperature() == null ? properties.getTemperature() : request.temperature())
                .maxTokens(request.maxOutputTokens() == null ? properties.getMaxOutputTokens() : request.maxOutputTokens());
        if (request.jsonResponse()) {
            builder.responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null));
        }
        return builder.build();
    }

    private List<Message> toSpringMessages(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("ai.messages.required");
        }
        List<Message> result = new ArrayList<>();
        for (AiChatMessage message : messages) {
            String role = message.role() == null ? "user" : message.role().toLowerCase(Locale.ROOT);
            String content = message.content() == null ? "" : message.content();
            result.add(switch (role) {
                case "system" -> new SystemMessage(content);
                case "assistant" -> new AssistantMessage(content);
                default -> toUserMessage(message, content);
            });
        }
        return result;
    }

    private UserMessage toUserMessage(AiChatMessage message, String content) {
        if (message.media() == null || message.media().isEmpty()) {
            return new UserMessage(content);
        }
        List<Media> media = message.media()
                .stream()
                .map(item -> Media.builder()
                        .mimeType(MimeTypeUtils.parseMimeType(item.contentType()))
                        .name(item.name())
                        .data(new ByteArrayResource(item.data() == null ? new byte[0] : item.data()))
                        .build())
                .toList();
        return UserMessage.builder().text(content).media(media).build();
    }
    private AiUsage extractUsage(ChatResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        Usage usage = metadata.getUsage();
        if (usage == null) {
            return null;
        }
        return new AiUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private void waitBeforeRetry(int completedAttempts, long deadlineNanos) {
        Duration initial = AiDefaults.RETRY_INITIAL_BACKOFF;
        Duration maximum = AiDefaults.RETRY_MAX_BACKOFF;
        long multiplier = 1L << Math.min(Math.max(0, completedAttempts - 1), 10);
        long exponentialMillis;
        try {
            exponentialMillis = Math.multiplyExact(initial.toMillis(), multiplier);
        } catch (ArithmeticException ex) {
            exponentialMillis = maximum.toMillis();
        }
        long cappedMillis = Math.min(maximum.toMillis(), exponentialMillis);
        long jitterBound = Math.max(1L, cappedMillis / 4L + 1L);
        long delayMillis = Math.min(maximum.toMillis(), cappedMillis + ThreadLocalRandom.current().nextLong(jitterBound));
        long remainingMillis = Math.max(0L, (deadlineNanos - System.nanoTime()) / 1_000_000L);
        if (remainingMillis <= 1L) {
            throw unavailable();
        }
        try {
            Thread.sleep(Math.min(delayMillis, remainingMillis - 1L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    private boolean deadlineReached(long deadlineNanos) {
        return System.nanoTime() >= deadlineNanos;
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("ai.provider.unavailable");
    }
}