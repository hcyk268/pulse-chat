package backend.xxx.chat.community.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.community.dto.CommunityCategoryResponse;
import backend.xxx.chat.community.dto.CommunityChannelResponse;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunitySummaryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.dto.CreateCommunityChannelRequest;
import backend.xxx.chat.community.dto.CreateCommunityRequest;
import backend.xxx.chat.community.dto.UpdateCommunityChannelRequest;
import backend.xxx.chat.community.dto.UpdateCommunityRequest;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityCategory;
import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityChannelStatus;
import backend.xxx.chat.community.model.CommunityChannelType;
import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberId;
import backend.xxx.chat.community.model.CommunityMemberRole;
import backend.xxx.chat.community.model.CommunityMemberStatus;
import backend.xxx.chat.community.model.CommunityStatus;
import backend.xxx.chat.community.model.CommunityTag;
import backend.xxx.chat.community.model.CommunityTagLink;
import backend.xxx.chat.community.model.CommunityVisibility;
import backend.xxx.chat.community.repository.CommunityCategoryRepository;
import backend.xxx.chat.community.repository.CommunityChannelRepository;
import backend.xxx.chat.community.repository.CommunityMemberRepository;
import backend.xxx.chat.community.repository.CommunityRepository;
import backend.xxx.chat.community.repository.CommunityTagLinkRepository;
import backend.xxx.chat.community.repository.CommunityTagRepository;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ParticipantStatus;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;
import backend.xxx.chat.notification.service.NotificationCommand;
import backend.xxx.chat.notification.service.NotificationService;
import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final UserLookupService userLookupService;
    private final CommunityCategoryRepository communityCategoryRepository;
    private final CommunityRepository communityRepository;
    private final CommunityTagRepository communityTagRepository;
    private final CommunityTagLinkRepository communityTagLinkRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityChannelRepository communityChannelRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final CommunityValidator communityValidator;
    private final CommunityAccessPolicy communityAccessPolicy;
    private final CommunityAssetResolver communityAssetResolver;
    private final CommunityResponseBuilder communityResponseBuilder;
    private final CommunityMapper communityMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommunityCategoryResponse> getCategories() {
        return communityCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(communityMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityTagResponse> getTags() {
        return communityTagRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(communityMapper::toTagResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryResponse> discoverCommunities(
            String currentUsername,
            Short limit,
            String categorySlug,
            String tagSlug,
            String search
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        int pageLimit = communityValidator.normalizeLimit(limit);
        String normalizedCategorySlug = communityValidator.normalizeOptionalSlug(categorySlug);
        String normalizedTagSlug = communityValidator.normalizeOptionalSlug(tagSlug);
        String searchPattern = communityValidator.normalizeSearchPattern(search);

        List<Community> communities = communityRepository.findDiscoverable(
                CommunityStatus.ACTIVE,
                normalizedCategorySlug,
                normalizedTagSlug,
                searchPattern,
                PageRequest.of(0, pageLimit)
        );

        return communityResponseBuilder.buildSummaries(communities, currentUser);
    }

    @Transactional(readOnly = true)
    public CommunityDetailResponse getCommunityDetail(String currentUsername, String slug) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Community community = communityAccessPolicy.requireCommunityBySlug(slug);
        return communityResponseBuilder.buildDetail(community, currentUser);
    }

    @Transactional
    public CommunityDetailResponse createCommunity(String currentUsername, CreateCommunityRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Instant now = Instant.now();

        String name = communityValidator.normalizeRequiredCommunityName(request.name());
        String description = communityValidator.normalizeOptionalCommunityDescription(request.description());
        CommunityCategory category = resolveCategory(request.categorySlug());
        UploadedAsset avatarAsset = communityAssetResolver.resolveOwnedReadyAsset(
                currentUser,
                request.avatarAssetId(),
                UploadPurpose.COMMUNITY_AVATAR,
                null
        );
        UploadedAsset coverAsset = communityAssetResolver.resolveOwnedReadyAsset(
                currentUser,
                request.coverAssetId(),
                UploadPurpose.COMMUNITY_COVER,
                null
        );
        CommunityVisibility visibility =
                request.visibility() == null ? CommunityVisibility.PUBLIC : request.visibility();
        String slug = buildUniqueCommunitySlug(name);

        Community community = communityRepository.save(Community.create(
                currentUser,
                category,
                slug,
                name,
                description,
                avatarAsset,
                coverAsset,
                visibility
        ));

        CommunityMember owner = communityMemberRepository.save(CommunityMember.owner(community, currentUser, now));
        community.incrementMemberCount();
        replaceTags(community, request.tagSlugs());

        List<CreateCommunityChannelRequest> channelRequests =
                communityValidator.normalizeCreateChannelRequests(request.channels());
        Set<String> reservedChannelSlugs = new LinkedHashSet<>();
        for (int index = 0; index < channelRequests.size(); index++) {
            CommunityChannel channel = createChannelInternal(
                    community,
                    currentUser,
                    channelRequests.get(index),
                    index == 0,
                    (index + 1) * 10,
                    List.of(owner),
                    reservedChannelSlugs
            );
            if (index == 0) {
                community.setDefaultChannel(channel);
            }
        }

        return communityResponseBuilder.buildDetail(community, currentUser, owner);
    }

    @Transactional
    public CommunityDetailResponse updateCommunity(
            String currentUsername,
            Long communityId,
            UpdateCommunityRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Community community = communityAccessPolicy.requireCommunityById(communityId);
        CommunityMember membership =
                communityAccessPolicy.requireCommunityManager(community.getId(), currentUser.getId());

        String name = request.name() == null
                ? community.getName()
                : communityValidator.normalizeRequiredCommunityName(request.name());
        String description = request.description() == null
                ? community.getDescription()
                : communityValidator.normalizeOptionalCommunityDescription(request.description());
        CommunityCategory category = request.categorySlug() == null
                ? community.getCategory()
                : resolveCategory(request.categorySlug());
        UploadedAsset avatarAsset = request.avatarAssetId() == null
                ? community.getAvatarAsset()
                : communityAssetResolver.resolveOwnedReadyAsset(
                        currentUser,
                        request.avatarAssetId(),
                        UploadPurpose.COMMUNITY_AVATAR,
                        community.getAvatarAsset()
                );
        UploadedAsset coverAsset = request.coverAssetId() == null
                ? community.getCoverAsset()
                : communityAssetResolver.resolveOwnedReadyAsset(
                        currentUser,
                        request.coverAssetId(),
                        UploadPurpose.COMMUNITY_COVER,
                        community.getCoverAsset()
                );
        CommunityVisibility visibility =
                request.visibility() == null ? community.getVisibility() : request.visibility();

        community.updateProfile(category, name, description, avatarAsset, coverAsset, visibility);
        if (request.tagSlugs() != null) {
            replaceTags(community, request.tagSlugs());
        }

        return communityResponseBuilder.buildDetail(community, currentUser, membership);
    }

    @Transactional
    public CommunityDetailResponse joinCommunity(String currentUsername, Long communityId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Community community = communityAccessPolicy.requireCommunityById(communityId);
        Instant now = Instant.now();
        CommunityMemberId memberId = new CommunityMemberId(community.getId(), currentUser.getId());
        CommunityMember member = communityMemberRepository.findById(memberId).orElse(null);
        boolean countMember = false;

        if (member == null) {
            member = communityMemberRepository.save(CommunityMember.active(community, currentUser, now));
            countMember = true;
        } else if (member.getStatus() == CommunityMemberStatus.BANNED) {
            throw new ForbiddenException("community.member.banned");
        } else if (!member.isActive()) {
            member.reactivate(now);
            countMember = true;
        }

        if (countMember) {
            community.incrementMemberCount();
            notifyCommunityOwnerAboutJoin(community, currentUser);
        }

        List<CommunityChannel> channels = communityChannelRepository.findByCommunityIdAndStatusWithConversation(
                community.getId(),
                CommunityChannelStatus.ACTIVE
        );
        syncConversationParticipants(channels, currentUser, member.getRole(), false);

        return communityResponseBuilder.buildDetail(community, currentUser, member);
    }

    @Transactional
    public void leaveCommunity(String currentUsername, Long communityId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Community community = communityAccessPolicy.requireCommunityById(communityId);
        CommunityMember member =
                communityAccessPolicy.requireActiveCommunityMember(community.getId(), currentUser.getId());
        if (member.isOwner()) {
            throw new ConflictException("community.owner.cannot.leave");
        }

        Instant now = Instant.now();
        member.markLeft(now);
        community.decrementMemberCount();

        List<CommunityChannel> channels = communityChannelRepository.findByCommunityIdAndStatusWithConversation(
                community.getId(),
                CommunityChannelStatus.ACTIVE
        );
        List<Long> conversationIds = channels.stream()
                .map(channel -> channel.getConversation().getId())
                .toList();
        if (!conversationIds.isEmpty()) {
            conversationParticipantRepository.markLeftByConversationIdInAndUserId(
                    conversationIds,
                    currentUser.getId(),
                    now,
                    ParticipantStatus.ACTIVE,
                    ParticipantStatus.LEFT
            );
        }
    }

    @Transactional
    public CommunityChannelResponse createChannel(
            String currentUsername,
            Long communityId,
            CreateCommunityChannelRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Community community = communityAccessPolicy.requireCommunityById(communityId);
        communityAccessPolicy.requireChannelManager(community.getId(), currentUser.getId());

        boolean defaultChannel = community.getDefaultChannel() == null;
        int sortOrder = communityChannelRepository.findMaxSortOrderByCommunityId(community.getId()) + 10;
        List<CommunityMember> activeMembers = communityMemberRepository.findByCommunityIdAndStatusWithUser(
                community.getId(),
                CommunityMemberStatus.ACTIVE
        );
        CommunityChannel channel = createChannelInternal(
                community,
                currentUser,
                request,
                defaultChannel,
                sortOrder,
                activeMembers,
                null
        );
        if (defaultChannel) {
            community.setDefaultChannel(channel);
        }

        return communityMapper.toChannelResponse(channel, 0L);
    }

    @Transactional
    public CommunityChannelResponse updateChannel(
            String currentUsername,
            Long channelId,
            UpdateCommunityChannelRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        CommunityChannel channel = communityChannelRepository.findByIdWithCommunityAndConversation(channelId)
                .orElseThrow(() -> new NotFoundException("community.channel.not.found"));
        communityAccessPolicy.requireChannelManager(channel.getCommunity().getId(), currentUser.getId());

        if (request.name() != null) {
            String name = communityValidator.normalizeRequiredChannelName(request.name());
            channel.setName(name);
            channel.getConversation().updateProfile(
                    buildConversationName(channel.getCommunity(), name),
                    communityAvatarUrl(channel.getCommunity())
            );
        }
        if (request.description() != null) {
            channel.setDescription(
                    communityValidator.normalizeOptionalChannelDescription(request.description())
            );
        }
        if (request.type() != null) {
            channel.setType(request.type());
        }
        if (request.readOnly() != null) {
            channel.setReadOnly(request.readOnly());
        }

        long unreadCount = conversationParticipantRepository.findById(
                        new ConversationParticipantId(channel.getConversation().getId(), currentUser.getId())
                )
                .map(ConversationParticipant::getUnreadCount)
                .orElse(0L);
        return communityMapper.toChannelResponse(channel, unreadCount);
    }

    private void notifyCommunityOwnerAboutJoin(Community community, User member) {
        if (community.getOwner().getId().equals(member.getId())) {
            return;
        }

        notificationService.create(new NotificationCommand(
                community.getOwner(),
                member,
                NotificationType.COMMUNITY,
                "New community member",
                member.getDisplayName() + " joined " + community.getName() + ".",
                NotificationTargetType.COMMUNITY,
                community.getId(),
                "COMMUNITY_MEMBER",
                member.getId(),
                null
        ));
    }

    private CommunityChannel createChannelInternal(
            Community community,
            User creator,
            CreateCommunityChannelRequest request,
            boolean defaultChannel,
            int sortOrder,
            List<CommunityMember> activeMembers,
            Set<String> reservedSlugs
    ) {
        String name =
                communityValidator.normalizeRequiredChannelName(request == null ? null : request.name());
        String description = communityValidator.normalizeOptionalChannelDescription(
                request == null ? null : request.description()
        );
        CommunityChannelType type =
                request == null || request.type() == null ? CommunityChannelType.TEXT : request.type();
        boolean readOnly = request != null && Boolean.TRUE.equals(request.readOnly());
        String slug = reservedSlugs == null
                ? buildUniqueChannelSlug(community.getId(), name)
                : reserveUniqueSlug(
                        communityValidator.normalizeRequiredSlug(name, "community.channel.slug"),
                        reservedSlugs
                );

        Conversation conversation = conversationRepository.save(Conversation.createGroupConversation(
                buildConversationName(community, name),
                communityAvatarUrl(community),
                creator
        ));

        conversationParticipantRepository.saveAll(activeMembers.stream()
                .map(member -> createConversationParticipant(
                        conversation,
                        member.getUser(),
                        member.getRole(),
                        false
                ))
                .toList());

        return communityChannelRepository.save(CommunityChannel.create(
                community,
                conversation,
                slug,
                name,
                description,
                type,
                sortOrder,
                defaultChannel,
                readOnly,
                creator
        ));
    }

    private void syncConversationParticipants(
            List<CommunityChannel> channels,
            User user,
            CommunityMemberRole communityRole,
            boolean visibleInList
    ) {
        if (channels.isEmpty()) {
            return;
        }

        List<Long> conversationIds = channels.stream()
                .map(channel -> channel.getConversation().getId())
                .toList();
        Map<Long, ConversationParticipant> participantByConversationId =
                conversationParticipantRepository.findByConversationIdInAndUserId(conversationIds, user.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                participant -> participant.getId().getConversationId(),
                                Function.identity()
                        ));
        List<ConversationParticipant> newParticipants = new ArrayList<>();

        for (CommunityChannel channel : channels) {
            Conversation conversation = channel.getConversation();
            ConversationParticipant participant = participantByConversationId.get(conversation.getId());
            if (participant == null) {
                newParticipants.add(createConversationParticipant(
                        conversation,
                        user,
                        communityRole,
                        visibleInList
                ));
                continue;
            }

            if (!participant.isActive()) {
                participant.acceptInvitation();
            }
            if (communityRole == CommunityMemberRole.OWNER) {
                participant.promoteToOwner();
            }
            if (!visibleInList) {
                participant.hideFromList();
            }
        }

        if (!newParticipants.isEmpty()) {
            conversationParticipantRepository.saveAll(newParticipants);
        }
    }

    private ConversationParticipant createConversationParticipant(
            Conversation conversation,
            User user,
            CommunityMemberRole communityRole,
            boolean visibleInList
    ) {
        ConversationParticipant participant =
                ConversationParticipant.create(conversation, user, visibleInList);
        if (communityRole == CommunityMemberRole.OWNER) {
            participant.promoteToOwner();
        }
        if (!visibleInList) {
            participant.hideFromList();
        }
        return participant;
    }

    private CommunityCategory resolveCategory(String categorySlug) {
        String normalizedSlug = communityValidator.normalizeOptionalSlug(categorySlug);
        if (normalizedSlug == null) {
            return null;
        }
        return communityCategoryRepository.findBySlugAndActiveTrue(normalizedSlug)
                .orElseThrow(() -> new NotFoundException("community.category.not.found"));
    }

    private void replaceTags(Community community, List<String> tagSlugs) {
        communityTagLinkRepository.deleteByCommunityId(community.getId());
        List<CommunityTag> tags = resolveTags(tagSlugs);
        if (tags.isEmpty()) {
            return;
        }
        communityTagLinkRepository.saveAll(tags.stream()
                .map(tag -> CommunityTagLink.create(community, tag))
                .toList());
    }

    private List<CommunityTag> resolveTags(List<String> tagSlugs) {
        Set<String> normalizedSlugs = communityValidator.normalizeSlugSet(tagSlugs);
        if (normalizedSlugs.isEmpty()) {
            return List.of();
        }

        List<CommunityTag> tags = communityTagRepository.findBySlugInAndActiveTrue(normalizedSlugs);
        Set<String> resolvedSlugs = tags.stream().map(CommunityTag::getSlug).collect(Collectors.toSet());
        Optional<String> missingSlug = normalizedSlugs.stream()
                .filter(slug -> !resolvedSlugs.contains(slug))
                .findFirst();
        if (missingSlug.isPresent()) {
            throw new NotFoundException("community.tag.not.found");
        }

        return tags.stream()
                .sorted(Comparator.comparing(CommunityTag::getName))
                .toList();
    }

    private String buildUniqueCommunitySlug(String name) {
        String baseSlug = communityValidator.normalizeRequiredSlug(name, "community.slug");
        return nextAvailableSlug(baseSlug, communityRepository.findSlugsByBase(baseSlug));
    }

    private String buildUniqueChannelSlug(Long communityId, String name) {
        String baseSlug = communityValidator.normalizeRequiredSlug(name, "community.channel.slug");
        return nextAvailableSlug(
                baseSlug,
                communityChannelRepository.findSlugsByCommunityIdAndBase(communityId, baseSlug)
        );
    }

    private String reserveUniqueSlug(String baseSlug, Set<String> reservedSlugs) {
        String slug = nextAvailableSlug(baseSlug, reservedSlugs);
        reservedSlugs.add(slug);
        return slug;
    }

    private String nextAvailableSlug(String baseSlug, java.util.Collection<String> existingSlugs) {
        Set<String> existing = new LinkedHashSet<>(existingSlugs);
        if (!existing.contains(baseSlug)) {
            return baseSlug;
        }

        int suffix = 2;
        while (existing.contains(baseSlug + "-" + suffix)) {
            suffix++;
        }
        return baseSlug + "-" + suffix;
    }

    private String buildConversationName(Community community, String channelName) {
        return community.getName() + " #" + channelName;
    }

    private String communityAvatarUrl(Community community) {
        if (community.getAvatarAsset() == null) {
            return null;
        }
        return community.getAvatarAsset().getPublicUrl();
    }
}
