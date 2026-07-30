package backend.xxx.chat.community.dto;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.community.model.CommunityStatus;
import backend.xxx.chat.community.model.CommunityVisibility;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record CommunitySummaryResponse(
        Long id,
        String slug,
        String name,
        String description,
        CommunityCategoryResponse category,
        List<CommunityTagResponse> tags,
        CommunityAssetResponse avatar,
        CommunityAssetResponse cover,
        SummarizeUserResponse owner,
        long memberCount,
        long onlineCount,
        CommunityVisibility visibility,
        CommunityStatus status,
        Long defaultChannelId,
        CommunityMembershipResponse membership,
        Instant createdAt,
        Instant updatedAt
) {
}
