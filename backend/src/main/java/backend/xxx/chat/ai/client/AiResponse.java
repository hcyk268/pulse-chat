package backend.xxx.chat.ai.client;

public record AiResponse(
        String content,
        String model,
        AiUsage usage
) {
}