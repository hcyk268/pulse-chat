package backend.xxx.chat.community.dto;

import java.time.Instant;

import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberRole;
import backend.xxx.chat.community.model.CommunityMemberStatus;
import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;

public record CommunityMemberResponse(
        SummarizeUserResponse user,
        CommunityMemberRole role,
        CommunityMemberStatus status,
        Instant joinedAt,
        Instant lastSeenAt,
        PresenceResponse presence
) {

    public static CommunityMemberResponse from(CommunityMember member, Presence presence) {
        User user = member.getUser();
        return new CommunityMemberResponse(
                new SummarizeUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl()),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLastSeenAt(),
                presence == null ? new PresenceResponse(false, null) : new PresenceResponse(presence.isOnline(), presence.getLastActiveAt())
        );
    }
}
