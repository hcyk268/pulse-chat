package backend.xxx.chat.conversation.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.util.CursorCodec;
import backend.xxx.chat.conversation.dto.*;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int DEFAULT_CONVERSATION_LIMIT = 20;
    private static final int MAX_CONVERSATION_LIMIT = 50;

    private final UserLookupService userLookupService;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationResponseBuilder conversationResponseBuilder;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final CursorCodec cursorCodec;

    @Transactional
    public CreateOrOpenDirectConversationResult createOrOpenDirectConversation(
            String currentUsername,
            CreateDirectConversationRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        User targetUser = userLookupService.getUser(request.targetUserId());
        validateDirectConversationTarget(currentUser, targetUser);

        return conversationParticipantRepository.findDirectConversationIdBetween(
                        currentUser.getId(),
                        targetUser.getId(),
                        ConversationType.DIRECT
                )
                .map(conversationId -> openExistingDirectConversation(conversationId, currentUser, targetUser))
                .orElseGet(() -> createDirectConversation(currentUser, targetUser));
    }

    @Transactional
    public ConversationDetailResponse createGroupConversation(
            String currentUsername,
            CreateGroupConversationRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        List<User> invitedUsers = resolveGroupInvitees(currentUser, request.memberIds(), 2);

        Conversation conversation = conversationRepository.save(Conversation.createGroupConversation(
                normalizeRequiredGroupName(request.name()),
                normalizeOptionalText(request.avatarUrl()),
                currentUser
        ));

        ConversationParticipant owner = ConversationParticipant.create(conversation, currentUser, true);
        owner.promoteToOwner();
        conversationParticipantRepository.save(owner);

        invitedUsers.forEach(user -> conversationParticipantRepository.save(
                ConversationParticipant.createPending(conversation, user, currentUser)
        ));

        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public ConversationDetailResponse inviteGroupMembers(
            String currentUsername,
            Long conversationId,
            AddGroupMembersRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        conversationAccessPolicy.requireActiveMember(conversationId, currentUser.getId());
        List<User> invitedUsers = resolveGroupInvitees(currentUser, request.memberIds(), 2);

        for (User invitedUser : invitedUsers) {
            ConversationParticipant participant = conversationParticipantRepository.findById(
                    new ConversationParticipantId(conversationId, invitedUser.getId())
            ).orElse(null);

            if (participant == null) {
                conversationParticipantRepository.save(ConversationParticipant.createPending(
                        conversation,
                        invitedUser,
                        currentUser
                ));
                continue;
            }

            if (participant.isActive() || participant.isPending()) {
                throw conflict("User is already a member or has a pending invitation");
            }

            participant.markPendingInvitation(currentUser);
        }

        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public ConversationDetailResponse acceptGroupInvitation(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        ConversationParticipant participant = conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        if (!participant.isPending()) {
            throw conflict("No pending group invitation found");
        }

        participant.acceptInvitation();
        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public void rejectGroupInvitation(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        conversationAccessPolicy.requireGroupConversation(conversationId);
        ConversationParticipant participant = conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        if (!participant.isPending()) {
            throw conflict("No pending group invitation found");
        }

        participant.markLeft(Instant.now());
        participant.hideFromList();
    }

    @Transactional
    public ConversationDetailResponse removeGroupMember(
            String currentUsername,
            Long conversationId,
            Long memberId
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        conversationAccessPolicy.requireOwner(conversationId, currentUser.getId());

        if (currentUser.getId().equals(memberId)) {
            throw new ValidationException("Owner must use leave group instead of remove member");
        }

        ConversationParticipant targetParticipant = conversationAccessPolicy.requireParticipant(conversationId, memberId);
        if (targetParticipant.isLeft()) {
            throw conflict("Member already left this group");
        }

        targetParticipant.markLeft(Instant.now());
        targetParticipant.hideFromList();
        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public void leaveGroup(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        conversationAccessPolicy.requireGroupConversation(conversationId);
        ConversationParticipant participant = conversationAccessPolicy.requireActiveMember(conversationId, currentUser.getId());

        participant.markLeft(Instant.now());
        participant.hideFromList();

        if (participant.getRole() == ParticipantRole.OWNER) {
            promoteNextOwnerIfNeeded(conversationId, currentUser.getId());
        }
    }

    @Transactional
    public ConversationDetailResponse updateGroupProfile(
            String currentUsername,
            Long conversationId,
            UpdateGroupProfileRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        conversationAccessPolicy.requireActiveMember(conversationId, currentUser.getId());

        String name = request.name() == null
                ? conversation.getName()
                : normalizeRequiredGroupName(request.name());
        String avatarUrl = request.avatarUrl() == null
                ? conversation.getAvatarUrl()
                : normalizeOptionalText(request.avatarUrl());

        conversation.updateProfile(name, avatarUrl);
        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public ConversationDetailResponse updateGroupMemberRole(
            String currentUsername,
            Long conversationId,
            Long memberId,
            UpdateGroupMemberRoleRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        conversationAccessPolicy.requireOwner(conversationId, currentUser.getId());

        ConversationParticipant targetParticipant = conversationAccessPolicy.requireActiveMember(conversationId, memberId);
        if (targetParticipant.getRole() == ParticipantRole.OWNER && request.role() == ParticipantRole.MEMBER
                && !hasAnotherActiveOwner(conversationId, memberId)) {
            throw conflict("Group must have at least one owner");
        }

        targetParticipant.changeRole(request.role());
        return buildConversationDetailResponse(conversation, currentUser);
    }

    private CreateOrOpenDirectConversationResult openExistingDirectConversation(
            Long conversationId,
            User currentUser,
            User targetUser
    ) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        ConversationParticipant currentParticipant = conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversation.getId(), currentUser.getId())
                )
                .orElseThrow(() -> new NotFoundException("Conversation participant not found"));
        currentParticipant.markVisibleInList();

        return new CreateOrOpenDirectConversationResult(
                conversationResponseBuilder.buildDirectConversationResponse(conversation, currentUser, targetUser),
                false
        );
    }

    private CreateOrOpenDirectConversationResult createDirectConversation(User currentUser, User targetUser) {
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());

        conversationParticipantRepository.save(ConversationParticipant.create(conversation, currentUser, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, targetUser, false));

        return new CreateOrOpenDirectConversationResult(
                conversationResponseBuilder.buildDirectConversationResponse(conversation, currentUser, targetUser),
                true
        );
    }

    @Transactional(readOnly = true)
    public ConversationBoxResponse getConversations(Short limit, String cursor, Instant snapshotAt, String currentUsername)  {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        int pageLimit = normalizeConversationLimit(limit);

        Instant snapshot = snapshotAt == null ? Instant.now() : snapshotAt;

        ConversationCursor conversationCursor = this.decodeCursor(cursor);

        PageRequest pageRequest = PageRequest.of(0, pageLimit + 1);
        List<ConversationParticipant> conversations = conversationCursor == null
                ? conversationParticipantRepository.findVisibleFirstPageByUserId(
                        currentUser.getId(),
                        snapshot,
                        pageRequest
                )
                : conversationParticipantRepository.findVisiblePageByUserIdAfterCursor(
                        currentUser.getId(),
                        snapshot,
                        conversationCursor.cursorAt(),
                        conversationCursor.conversationId(),
                        pageRequest
                );

        boolean hasMore = conversations.size() > pageLimit;
        List<ConversationParticipant> page = hasMore ? conversations.subList(0, pageLimit) : conversations;

        if (page.isEmpty()) {
            return new ConversationBoxResponse(
                    List.of(),
                    new CursorPageResponse(pageLimit, null, false, snapshot)
            );
        }

        String nextCursor = null;
        if (hasMore) {
            ConversationParticipant lastConversation = page.get(page.size() - 1);
            Conversation conversation = lastConversation.getConversation();
            nextCursor = this.buildConversationNextCursor(
                    new ConversationCursor(getConversationSortAt(conversation), conversation.getId()));
        }
        CursorPageResponse cursorPageResponse = new CursorPageResponse(pageLimit, nextCursor, hasMore, snapshot);

        List<ConversationResponse> items = conversationResponseBuilder.buildForCurrentUser(page, currentUser);

        return new ConversationBoxResponse(
                items,
                cursorPageResponse
        );
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetailConversation(Long conversationId, String currentUsername) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        List<ConversationParticipant> participants =
                conversationAccessPolicy.requireParticipants(conversationId);
        conversationAccessPolicy.assertCanReadConversation(currentUser, participants);

        return conversationResponseBuilder.buildConversationDetailResponse(
                conversation,
                currentUser,
                participants
        );
    }

    private ConversationDetailResponse buildConversationDetailResponse(Conversation conversation, User currentUser) {
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversation.getId());
        return conversationResponseBuilder.buildConversationDetailResponse(conversation, currentUser, participants);
    }

    private List<User> resolveGroupInvitees(User currentUser, List<Long> memberIds, int minDistinctMembers) {
        Set<Long> distinctMemberIds = new LinkedHashSet<>(memberIds == null ? List.of() : memberIds);
        if (distinctMemberIds.contains(currentUser.getId())) {
            throw new ValidationException("memberIds must not contain the current user");
        }

        if (distinctMemberIds.size() < minDistinctMembers) {
            throw new ValidationException("memberIds must contain at least " + minDistinctMembers + " distinct users");
        }

        List<User> users = userRepository.findAllById(distinctMemberIds);
        if (users.size() != distinctMemberIds.size()) {
            throw new NotFoundException("One or more users not found");
        }

        users.forEach(user -> {
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new ValidationException("All group members must be active users");
            }
        });

        return users;
    }

    private void promoteNextOwnerIfNeeded(Long conversationId, Long leavingUserId) {
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        boolean hasOwner = participants.stream()
                .anyMatch(participant -> participant.isActive()
                        && participant.getRole() == ParticipantRole.OWNER
                        && !participant.getUser().getId().equals(leavingUserId));
        if (hasOwner) {
            return;
        }

        participants.stream()
                .filter(participant -> participant.isActive()
                        && !participant.getUser().getId().equals(leavingUserId))
                .min((left, right) -> left.getJoinedAt().compareTo(right.getJoinedAt()))
                .ifPresent(ConversationParticipant::promoteToOwner);
    }

    private boolean hasAnotherActiveOwner(Long conversationId, Long userId) {
        return conversationParticipantRepository.findByConversationIdWithUser(conversationId)
                .stream()
                .anyMatch(participant -> participant.isActive()
                        && participant.getRole() == ParticipantRole.OWNER
                        && !participant.getUser().getId().equals(userId));
    }

    private String normalizeRequiredGroupName(String name) {
        String normalized = normalizeOptionalText(name);
        if (normalized == null) {
            throw new ValidationException("Group name must not be blank");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ConflictException conflict(String message) {
        return new ConflictException(message);
    }

    private String buildConversationNextCursor(ConversationCursor conversationCursor) {
        return cursorCodec.encode(conversationCursor, "Failed to build conversation cursor");
    }

    private ConversationCursor decodeCursor(String cursor) {
        ConversationCursor conversationCursor =
                cursorCodec.decode(cursor, ConversationCursor.class, "Invalid conversation cursor");

        if (conversationCursor != null
                && (conversationCursor.cursorAt() == null || conversationCursor.conversationId() == null)) {
            throw new ValidationException("Invalid conversation cursor");
        }

        return conversationCursor;
    }

    private int normalizeConversationLimit(Short limit) {
        int pageLimit = limit == null ? DEFAULT_CONVERSATION_LIMIT : limit;

        if (pageLimit < 1 || pageLimit > MAX_CONVERSATION_LIMIT) {
            throw new ValidationException("limit must be between 1 and " + MAX_CONVERSATION_LIMIT);
        }

        return pageLimit;
    }

    private Instant getConversationSortAt(Conversation conversation) {
        return conversation.getLastMessageAt() == null
                ? conversation.getCreatedAt()
                : conversation.getLastMessageAt();
    }

    public record CreateOrOpenDirectConversationResult(
            DirectConversationResponse response,
            boolean created
    ) {
    }

    private void validateDirectConversationTarget(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ValidationException("targetUserId must not be the current user");
        }

        if (targetUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Target user is not active");
        }
    }

}