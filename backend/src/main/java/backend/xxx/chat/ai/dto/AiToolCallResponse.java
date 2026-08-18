package backend.xxx.chat.ai.dto;

public record AiToolCallResponse(
        String tool,
        String argumentsJson
) {
}