package backend.xxx.chat.community.dto;

import java.time.Instant;

import backend.xxx.chat.community.model.CommunityMemberRole;
import backend.xxx.chat.community.model.CommunityMemberStatus;

public record CommunityMembershipResponse(
        boolean member,
        CommunityMemberRole role,
        CommunityMemberStatus status,
        Instant joinedAt
) {
}
