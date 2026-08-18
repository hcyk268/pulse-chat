package backend.xxx.chat.ai.dto;

import java.time.Instant;
import java.util.List;

public record ConversationSummaryResponse(
        Long conversationId,
        String summary,
        List<String> highlights,
        List<String> actionItems,
        Instant generatedAt,
        String model
) {
}