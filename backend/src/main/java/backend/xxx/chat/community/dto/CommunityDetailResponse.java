package backend.xxx.chat.community.dto;

import java.util.List;

public record CommunityDetailResponse(
        CommunitySummaryResponse community,
        List<CommunityChannelResponse> channels,
        List<CommunityMemberResponse> members
) {
}
