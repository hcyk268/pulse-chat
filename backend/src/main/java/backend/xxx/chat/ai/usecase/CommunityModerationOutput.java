package backend.xxx.chat.ai.usecase;

import backend.xxx.chat.ai.dto.CommunityModerationDecision;
import java.util.List;

public record CommunityModerationOutput(
        CommunityModerationDecision decision,
        String reason,
        String categorySlug,
        List<String> suggestedTags
) {
}