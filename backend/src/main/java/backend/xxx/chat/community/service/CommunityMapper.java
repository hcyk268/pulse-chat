package backend.xxx.chat.community.service;

import java.util.List;
import java.util.Map;

import backend.xxx.chat.community.dto.CommunityAssetResponse;
import backend.xxx.chat.community.dto.CommunityCategoryResponse;
import backend.xxx.chat.community.dto.CommunityChannelResponse;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunityMemberResponse;
import backend.xxx.chat.community.dto.CommunityMembershipResponse;
import backend.xxx.chat.community.dto.CommunitySummaryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityCategory;
import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityTag;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {

    public CommunityCategoryResponse toCategoryResponse(CommunityCategory category) {
        return new CommunityCategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder()
        );
    }

    public CommunityTagResponse toTagResponse(CommunityTag tag) {
        return new CommunityTagResponse(
                tag.getId(),
                tag.getSlug(),
                tag.getName()
        );
    }

    public CommunitySummaryResponse toSummary(
            Community community,
            List<CommunityTagResponse> tags,
            CommunityMember membership,
            long onlineCount
    ) {
        User owner = community.getOwner();
        Long defaultChannelId = community.getDefaultChannel() == null ? null : community.getDefaultChannel().getId();
        return new CommunitySummaryResponse(
                community.getId(),
                community.getSlug(),
                community.getName(),
                community.getDescription(),
                community.getCategory() == null ? null : toCategoryResponse(community.getCategory()),
                tags,
                toAssetResponse(community.getAvatarAsset()),
                toAssetResponse(community.getCoverAsset()),
                toSummaryUserResponse(owner),
                community.getMemberCount(),
                onlineCount,
                community.getVisibility(),
                community.getStatus(),
                defaultChannelId,
                toMembershipResponse(membership),
                community.getCreatedAt(),
                community.getUpdatedAt()
        );
    }

    public CommunityDetailResponse toDetail(
            Community community,
            List<CommunityTagResponse> tags,
            CommunityMember membership,
            long onlineCount,
            List<CommunityChannel> channels,
            Map<Long, Long> unreadCountByConversationId,
            List<CommunityMember> members,
            Map<Long, Presence> presenceByUserId
    ) {
        List<CommunityChannelResponse> channelResponses = channels.stream()
                .map(channel -> toChannelResponse(
                        channel,
                        unreadCountByConversationId.getOrDefault(channel.getConversation().getId(), 0L)
                ))
                .toList();

        List<CommunityMemberResponse> memberResponses = members.stream()
                .map(member -> toMemberResponse(
                        member,
                        presenceByUserId.get(member.getUser().getId())
                ))
                .toList();

        return new CommunityDetailResponse(
                toSummary(community, tags, membership, onlineCount),
                channelResponses,
                memberResponses
        );
    }

    public CommunityChannelResponse toChannelResponse(CommunityChannel channel, long unreadCount) {
        return new CommunityChannelResponse(
                channel.getId(),
                channel.getSlug(),
                channel.getName(),
                channel.getDescription(),
                channel.getType(),
                channel.getSortOrder(),
                channel.isDefaultChannel(),
                channel.isReadOnly(),
                channel.getStatus(),
                channel.getConversation().getId(),
                unreadCount,
                channel.getConversation().getLastMessageAt(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }

    private CommunityMemberResponse toMemberResponse(CommunityMember member, Presence presence) {
        return new CommunityMemberResponse(
                toSummaryUserResponse(member.getUser()),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLastSeenAt(),
                toPresenceResponse(presence)
        );
    }

    private CommunityMembershipResponse toMembershipResponse(CommunityMember member) {
        if (member == null) {
            return new CommunityMembershipResponse(false, null, null, null);
        }
        return new CommunityMembershipResponse(
                member.isActive(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }

    private CommunityAssetResponse toAssetResponse(UploadedAsset asset) {
        if (asset == null) {
            return null;
        }
        return new CommunityAssetResponse(
                asset.getId(),
                asset.getPublicUrl(),
                asset.getThumbnailUrl(),
                asset.getFileName(),
                asset.getContentType()
        );
    }

    private SummarizeUserResponse toSummaryUserResponse(User user) {
        return new SummarizeUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    private PresenceResponse toPresenceResponse(Presence presence) {
        return presence == null
                ? new PresenceResponse(false, null)
                : new PresenceResponse(presence.isOnline(), presence.getLastActiveAt());
    }
}
