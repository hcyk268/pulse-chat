package backend.xxx.chat.community.dto;

import java.time.Instant;

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
}
