package backend.xxx.chat.conversation.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.util.CursorCodec;
import backend.xxx.chat.conversation.dto.*;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
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
    private final ConversationValidator conversationValidator;
    private final CursorCodec cursorCodec;

    @Transactional
    public CreateOrOpenDirectConversationResult createOrOpenDirectConversation(
            String currentUsername,
            CreateDirectConversationRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        User targetUser = userLookupService.getUser(request.targetUserId());
        conversationValidator.validateDirectConversationTarget(currentUser, targetUser);

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
                conversationValidator.normalizeRequiredGroupName(request.name()),
                conversationValidator.normalizeOptionalText(request.avatarUrl()),
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
        List<User> invitedUsers = resolveGroupInvitees(currentUser, request.memberIds(), 1);

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

            conversationValidator.validateCanInviteParticipant(participant);
            participant.markPendingInvitation(currentUser);
        }

        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public ConversationDetailResponse acceptGroupInvitation(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Conversation conversation = conversationAccessPolicy.requireGroupConversation(conversationId);
        ConversationParticipant participant = conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        conversationValidator.validatePendingInvitation(participant);

        participant.acceptInvitation();
        return buildConversationDetailResponse(conversation, currentUser);
    }

    @Transactional
    public void rejectGroupInvitation(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        conversationAccessPolicy.requireGroupConversation(conversationId);
        ConversationParticipant participant = conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        conversationValidator.validatePendingInvitation(participant);

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

        conversationValidator.validateOwnerCannotRemoveSelf(currentUser.getId(), memberId);

        ConversationParticipant targetParticipant = conversationAccessPolicy.requireParticipant(conversationId, memberId);
        conversationValidator.validateMemberNotLeft(targetParticipant);

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
                : conversationValidator.normalizeRequiredGroupName(request.name());
        String avatarUrl = request.avatarUrl() == null
                ? conversation.getAvatarUrl()
                : conversationValidator.normalizeOptionalText(request.avatarUrl());

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
        conversationValidator.validateRoleChange(
                targetParticipant.getRole(),
                request.role(),
                hasAnotherActiveOwner(conversationId, memberId)
        );

        targetParticipant.changeRole(request.role());
        return buildConversationDetailResponse(conversation, currentUser);
    }

    private CreateOrOpenDirectConversationResult openExistingDirectConversation(
            Long conversationId,
            User currentUser,
            User targetUser
    ) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));

        ConversationParticipant currentParticipant = conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversation.getId(), currentUser.getId())
                )
                .orElseThrow(() -> new NotFoundException("conversation.participant.not.found"));
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
        int pageLimit = conversationValidator.normalizeConversationLimit(
                limit,
                DEFAULT_CONVERSATION_LIMIT,
                MAX_CONVERSATION_LIMIT
        );

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
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));

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
        Set<Long> distinctMemberIds = conversationValidator.validateMemberIds(
                currentUser.getId(),
                memberIds,
                minDistinctMembers
        );

        List<User> users = userRepository.findAllById(distinctMemberIds);
        conversationValidator.validateResolvedInvitees(distinctMemberIds, users);
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

    private String buildConversationNextCursor(ConversationCursor conversationCursor) {
        return cursorCodec.encode(conversationCursor, "conversation.cursor.build.failed");
    }

    private ConversationCursor decodeCursor(String cursor) {
        ConversationCursor conversationCursor =
                cursorCodec.decode(cursor, ConversationCursor.class, "conversation.cursor.invalid");

        conversationValidator.validateConversationCursor(conversationCursor);

        return conversationCursor;
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

}