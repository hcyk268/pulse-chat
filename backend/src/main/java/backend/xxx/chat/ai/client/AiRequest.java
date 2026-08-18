package backend.xxx.chat.ai.client;

import java.util.List;

public record AiRequest(
        List<AiChatMessage> messages,
        Integer maxOutputTokens,
        Double temperature,
        boolean jsonResponse
) {
}