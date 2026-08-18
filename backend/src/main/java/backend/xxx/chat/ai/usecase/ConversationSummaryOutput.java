package backend.xxx.chat.ai.usecase;

import java.util.List;

public record ConversationSummaryOutput(
        String summary,
        List<String> highlights,
        List<String> actionItems
) {
}