package backend.xxx.chat.conversation.service;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.util.CursorCodec;
import backend.xxx.chat.conversation.dto.*;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int DEFAULT_CONVERSATION_LIMIT = 20;
    private static final int MAX_CONVERSATION_LIMIT = 50;

    private final UserLookupService userLookupService;
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

    private CreateOrOpenDirectConversationResult openExistingDirectConversation(
            Long conversationId,
            User currentUser,
            User targetUser
    ) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        ConversationParticipant currentParticipant = conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversation.getId(), currentUser.getId())
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation participant not found"
                ));
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
    public DirectConversationResponse getDetailConversation(Long conversationId, String currentUsername) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Conversation not found"
        ));

        if (conversation.getType() != ConversationType.DIRECT) {
            throw new ValidationException("Conversation is not a direct conversation");
        }

        List<ConversationParticipant> participants = conversationAccessPolicy.requireParticipants(conversationId);
        conversationAccessPolicy.assertCanReadConversation(currentUser, participants);

        User targetUser = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User chat not found"
                ));

        return conversationResponseBuilder.buildDirectConversationResponse(conversation, currentUser, targetUser);
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
