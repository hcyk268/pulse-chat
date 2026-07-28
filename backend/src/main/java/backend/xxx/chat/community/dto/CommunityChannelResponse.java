package backend.xxx.chat.community.dto;

import java.time.Instant;

import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityChannelStatus;
import backend.xxx.chat.community.model.CommunityChannelType;

public record CommunityChannelResponse(
        Long id,
        String slug,
        String name,
        String description,
        CommunityChannelType type,
        int sortOrder,
        boolean defaultChannel,
        boolean readOnly,
        CommunityChannelStatus status,
        Long conversationId,
        long unreadCount,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static CommunityChannelResponse from(CommunityChannel channel, long unreadCount) {
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
}
