package backend.xxx.chat.community.service;

import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberId;
import backend.xxx.chat.community.model.CommunityVisibility;
import backend.xxx.chat.community.repository.CommunityMemberRepository;
import backend.xxx.chat.community.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityAccessPolicy {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityValidator communityValidator;

    public Community requireCommunityBySlug(String slug) {
        String normalizedSlug = communityValidator.normalizeRequiredSlug(slug, "community.slug");
        Community community = communityRepository.findBySlugWithDetails(normalizedSlug)
                .orElseThrow(() -> new NotFoundException("community.not.found"));
        communityValidator.validateActiveCommunity(community);
        return community;
    }

    public Community requireCommunityById(Long communityId) {
        communityValidator.validateCommunityId(communityId);
        Community community = communityRepository.findByIdWithDetails(communityId)
                .orElseThrow(() -> new NotFoundException("community.not.found"));
        communityValidator.validateActiveCommunity(community);
        return community;
    }

    public void requireCanViewCommunity(Community community, CommunityMember membership) {
        if (community.getVisibility() == CommunityVisibility.PRIVATE
                && (membership == null || !membership.isActive())) {
            throw new ForbiddenException("community.private.member.required");
        }
    }

    public CommunityMember requireActiveCommunityMember(Long communityId, Long userId) {
        CommunityMember member = communityMemberRepository.findById(new CommunityMemberId(communityId, userId))
                .orElseThrow(() -> new ForbiddenException("community.member.required"));
        if (!member.isActive()) {
            throw new ForbiddenException("community.active.member.required");
        }
        return member;
    }

    public CommunityMember requireCommunityManager(Long communityId, Long userId) {
        CommunityMember member = requireActiveCommunityMember(communityId, userId);
        if (!member.canManageCommunity()) {
            throw new ForbiddenException("community.manager.required");
        }
        return member;
    }

    public CommunityMember requireChannelManager(Long communityId, Long userId) {
        CommunityMember member = requireActiveCommunityMember(communityId, userId);
        if (!member.canManageChannels()) {
            throw new ForbiddenException("community.channel.manager.required");
        }
        return member;
    }
}
