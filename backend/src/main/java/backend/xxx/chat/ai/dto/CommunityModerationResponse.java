package backend.xxx.chat.ai.dto;

import java.time.Instant;
import java.util.List;

public record CommunityModerationResponse(
        CommunityModerationDecision decision,
        String reason,
        String categorySlug,
        List<String> suggestedTags,
        Instant generatedAt,
        String model
) {
}