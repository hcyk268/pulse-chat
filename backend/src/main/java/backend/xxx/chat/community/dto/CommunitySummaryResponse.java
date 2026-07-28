package backend.xxx.chat.community.dto;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityStatus;
import backend.xxx.chat.community.model.CommunityVisibility;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;

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

    public static CommunitySummaryResponse from(
            Community community,
            List<CommunityTagResponse> tags,
            CommunityMembershipResponse membership,
            long onlineCount
    ) {
        User owner = community.getOwner();
        Long defaultChannelId = community.getDefaultChannel() == null ? null : community.getDefaultChannel().getId();
        return new CommunitySummaryResponse(
                community.getId(),
                community.getSlug(),
                community.getName(),
                community.getDescription(),
                community.getCategory() == null ? null : CommunityCategoryResponse.from(community.getCategory()),
                tags,
                CommunityAssetResponse.from(community.getAvatarAsset()),
                CommunityAssetResponse.from(community.getCoverAsset()),
                new SummarizeUserResponse(owner.getId(), owner.getUsername(), owner.getDisplayName(), owner.getAvatarUrl()),
                community.getMemberCount(),
                onlineCount,
                community.getVisibility(),
                community.getStatus(),
                defaultChannelId,
                membership,
                community.getCreatedAt(),
                community.getUpdatedAt()
        );
    }
}
