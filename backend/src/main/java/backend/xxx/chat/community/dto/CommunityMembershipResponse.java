package backend.xxx.chat.community.dto;

import java.time.Instant;

import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberRole;
import backend.xxx.chat.community.model.CommunityMemberStatus;

public record CommunityMembershipResponse(
        boolean member,
        CommunityMemberRole role,
        CommunityMemberStatus status,
        Instant joinedAt
) {

    public static CommunityMembershipResponse from(CommunityMember member) {
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
}
