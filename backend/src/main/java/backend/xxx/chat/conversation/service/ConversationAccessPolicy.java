package backend.xxx.chat.conversation.service;

import java.util.List;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationAccessPolicy {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public List<ConversationParticipant> requireParticipants(Long conversationId) {
        validateConversationId(conversationId);

        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        if (participants.isEmpty() && !conversationRepository.existsById(conversationId)) {
            throw conversationNotFound();
        }

        return participants;
    }

    public ConversationParticipant requireParticipant(Long conversationId, Long userId) {
        validateConversationId(conversationId);
        if (userId == null) {
            throw new ValidationException("userId must not be null");
        }

        return conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, userId)
                )
                .orElseThrow(() -> {
                    if (!conversationRepository.existsById(conversationId)) {
                        return conversationNotFound();
                    }
                    return forbidden("You are not allowed to access this conversation");
                });
    }

    public void assertCanSendMessage(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw forbidden("You are not allowed to send message to this conversation");
        }
    }

    public void assertCanReadConversation(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw forbidden("You are not allowed to access this conversation");
        }
    }

    public void assertCanUpdateTyping(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw forbidden("You are not allowed to update typing status for this conversation");
        }
    }

    public void assertCanUpdateMessageStatus(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw forbidden("You are not allowed to update this message status");
        }
    }

    private boolean isParticipant(User user, List<ConversationParticipant> participants) {
        if (user == null || user.getId() == null || participants == null) {
            return false;
        }

        return participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(user.getId()));
    }

    private void validateConversationId(Long conversationId) {
        if (conversationId == null) {
            throw new ValidationException("conversationId must not be null");
        }
    }

    private ApiException conversationNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                "Conversation not found"
        );
    }

    private ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                message
        );
    }
}
