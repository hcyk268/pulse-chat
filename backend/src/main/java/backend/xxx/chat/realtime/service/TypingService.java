package backend.xxx.chat.realtime.service;

import java.util.List;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.model.TypingUpdatedEventData;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final UserLookupService userLookupService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Transactional(readOnly = true)
    public void updateTyping(String currentUsername, Long conversationId, TypingStatusRequest request) {
        validateRequest(conversationId, request);

        User currentUser = userLookupService.getCurrentUser(currentUsername);

        if (!conversationRepository.existsById(conversationId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    "Conversation not found"
            );
        }

        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        boolean isCurrentUserParticipant = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(currentUser.getId()));

        if (!isCurrentUserParticipant) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "You are not allowed to update typing status for this conversation"
            );
        }

        TypingUpdatedEventData data = new TypingUpdatedEventData(
                currentUser.getId(),
                currentUser.getUsername(),
                request.typing()
        );

        participants.stream()
                .filter(participant -> !participant.getUser().getId().equals(currentUser.getId()))
                .map(participant -> participant.getUser().getUsername())
                .forEach(username -> realtimeEventPublisher.sendToUser(
                        username,
                        RealtimeEventType.TYPING_UPDATED,
                        conversationId,
                        data
                ));
    }

    private void validateRequest(Long conversationId, TypingStatusRequest request) {
        if (conversationId == null) {
            throw new ValidationException("conversationId must not be null");
        }

        if (request == null || request.typing() == null) {
            throw new ValidationException("typing must not be null");
        }
    }
}
