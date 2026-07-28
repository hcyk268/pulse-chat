package backend.xxx.chat.community.service;

import java.util.List;
import java.util.Map;

import backend.xxx.chat.community.dto.CommunityChannelResponse;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunityMemberResponse;
import backend.xxx.chat.community.dto.CommunityMembershipResponse;
import backend.xxx.chat.community.dto.CommunitySummaryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.user.model.Presence;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {

    public CommunitySummaryResponse toSummary(
            Community community,
            List<CommunityTagResponse> tags,
            CommunityMember membership,
            long onlineCount
    ) {
        return CommunitySummaryResponse.from(
                community,
                tags,
                CommunityMembershipResponse.from(membership),
                onlineCount
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
                .map(channel -> CommunityChannelResponse.from(
                        channel,
                        unreadCountByConversationId.getOrDefault(channel.getConversation().getId(), 0L)
                ))
                .toList();

        List<CommunityMemberResponse> memberResponses = members.stream()
                .map(member -> CommunityMemberResponse.from(
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
}
