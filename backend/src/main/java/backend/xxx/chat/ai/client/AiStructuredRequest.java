package backend.xxx.chat.ai.client;

import java.util.List;

public record AiStructuredRequest<T>(
        List<AiChatMessage> messages,
        Integer maxOutputTokens,
        Double temperature,
        Class<T> outputType,
        boolean nativeStructuredOutput
) {
}