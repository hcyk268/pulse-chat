package backend.xxx.chat.community.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunitySummaryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityChannelStatus;
import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberId;
import backend.xxx.chat.community.model.CommunityMemberStatus;
import backend.xxx.chat.community.repository.CommunityChannelRepository;
import backend.xxx.chat.community.repository.CommunityMemberRepository;
import backend.xxx.chat.community.repository.CommunityTagLinkRepository;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.PresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityResponseBuilder {

    private static final int MAX_MEMBER_PREVIEW = 50;

    private final CommunityTagLinkRepository communityTagLinkRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityChannelRepository communityChannelRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final PresenceRepository presenceRepository;
    private final CommunityAccessPolicy communityAccessPolicy;
    private final CommunityMapper communityMapper;

    public List<CommunitySummaryResponse> buildSummaries(List<Community> communities, User currentUser) {
        if (communities.isEmpty()) {
            return List.of();
        }

        List<Long> communityIds = communities.stream().map(Community::getId).toList();
        Map<Long, List<CommunityTagResponse>> tagsByCommunityId = tagsByCommunityId(communityIds);
        Map<Long, CommunityMember> membershipByCommunityId = communityMemberRepository
                .findByCommunityIdInAndUserIdWithCommunity(communityIds, currentUser.getId())
                .stream()
                .collect(Collectors.toMap(member -> member.getCommunity().getId(), Function.identity()));
        Map<Long, Long> onlineCounts = onlineCountsByCommunityId(communityIds);

        return communities.stream()
                .map(community -> communityMapper.toSummary(
                        community,
                        tagsByCommunityId.getOrDefault(community.getId(), List.of()),
                        membershipByCommunityId.get(community.getId()),
                        onlineCounts.getOrDefault(community.getId(), 0L)
                ))
                .toList();
    }

    public CommunityDetailResponse buildDetail(Community community, User currentUser) {
        CommunityMember membership = communityMemberRepository
                .findById(new CommunityMemberId(community.getId(), currentUser.getId()))
                .orElse(null);
        return buildDetail(community, currentUser, membership);
    }

    public CommunityDetailResponse buildDetail(
            Community community,
            User currentUser,
            CommunityMember membership
    ) {
        communityAccessPolicy.requireCanViewCommunity(community, membership);

        List<CommunityChannel> channels = communityChannelRepository.findByCommunityIdAndStatusWithConversation(
                community.getId(),
                CommunityChannelStatus.ACTIVE
        );
        List<CommunityMember> members = communityMemberRepository.findPageByCommunityIdAndStatusWithUser(
                community.getId(),
                CommunityMemberStatus.ACTIVE,
                PageRequest.of(0, MAX_MEMBER_PREVIEW)
        );

        Map<Long, Long> unreadCounts = unreadCountsByConversationId(channels, currentUser.getId());
        Map<Long, Presence> presences = presenceByUserId(members.stream()
                .map(member -> member.getUser().getId())
                .toList());
        List<CommunityTagResponse> tags = tagsByCommunityId(List.of(community.getId()))
                .getOrDefault(community.getId(), List.of());
        long onlineCount = onlineCountsByCommunityId(List.of(community.getId()))
                .getOrDefault(community.getId(), 0L);

        return communityMapper.toDetail(
                community,
                tags,
                membership,
                onlineCount,
                channels,
                unreadCounts,
                members,
                presences
        );
    }

    private Map<Long, List<CommunityTagResponse>> tagsByCommunityId(Collection<Long> communityIds) {
        if (communityIds == null || communityIds.isEmpty()) {
            return Map.of();
        }
        return communityTagLinkRepository.findByCommunityIdInWithTag(communityIds)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getCommunity().getId(),
                        Collectors.mapping(link -> CommunityTagResponse.from(link.getTag()), Collectors.toList())
                ));
    }

    private Map<Long, Long> onlineCountsByCommunityId(Collection<Long> communityIds) {
        if (communityIds == null || communityIds.isEmpty()) {
            return Map.of();
        }
        return communityMemberRepository.countOnlineMembersByCommunityIds(
                        communityIds,
                        CommunityMemberStatus.ACTIVE
                )
                .stream()
                .collect(Collectors.toMap(
                        CommunityMemberRepository.CommunityOnlineCount::getCommunityId,
                        CommunityMemberRepository.CommunityOnlineCount::getOnlineCount
                ));
    }

    private Map<Long, Long> unreadCountsByConversationId(List<CommunityChannel> channels, Long userId) {
        if (channels == null || channels.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = channels.stream()
                .map(channel -> channel.getConversation().getId())
                .toList();
        return conversationParticipantRepository.findByConversationIdInAndUserId(conversationIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        participant -> participant.getConversation().getId(),
                        ConversationParticipant::getUnreadCount
                ));
    }

    private Map<Long, Presence> presenceByUserId(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return presenceRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Presence::getUserId, Function.identity()));
    }
}
