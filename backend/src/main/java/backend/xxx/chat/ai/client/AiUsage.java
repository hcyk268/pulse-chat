package backend.xxx.chat.ai.client;

public record AiUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}