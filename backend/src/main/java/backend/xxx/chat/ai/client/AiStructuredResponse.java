package backend.xxx.chat.ai.client;

public record AiStructuredResponse<T>(
        T entity,
        String model,
        AiUsage usage
) {
}