package backend.xxx.chat.ai.dto;

import java.time.Instant;
import java.util.List;

public record SmartAssistantResponse(
        String answer,
        List<AiToolCallResponse> toolCalls,
        Instant generatedAt,
        String model
) {
}