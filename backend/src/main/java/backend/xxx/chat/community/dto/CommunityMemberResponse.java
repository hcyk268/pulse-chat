package backend.xxx.chat.community.dto;

import java.time.Instant;

import backend.xxx.chat.community.model.CommunityMemberRole;
import backend.xxx.chat.community.model.CommunityMemberStatus;
import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record CommunityMemberResponse(
        SummarizeUserResponse user,
        CommunityMemberRole role,
        CommunityMemberStatus status,
        Instant joinedAt,
        Instant lastSeenAt,
        PresenceResponse presence
) {
}
