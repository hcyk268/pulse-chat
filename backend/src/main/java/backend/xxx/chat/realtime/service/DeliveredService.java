package backend.xxx.chat.realtime.service;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.realtime.event.MessageDeliveredDomainEvent;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveredService {

    private final UserLookupService userLookupService;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void messageDelivered(String currentUsername, Long messageId) {
        validateRequest(currentUsername, messageId);

        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        Long conversationId = message.getConversation().getId();
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        boolean isCurrentUserParticipant = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(currentUser.getId()));

        if (!isCurrentUserParticipant) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "You are not allowed to update this message status"
            );
        }

        if (message.getSender().getId().equals(currentUser.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Sender cannot mark their own message as delivered"
            );
        }

        Instant deliveredAt = Instant.now();
        int updatedCount = messageRepository.markMessagesDeliveredUpTo(
                conversationId,
                currentUser.getId(),
                message.getCreatedAt(),
                message.getId(),
                MessageStatus.SENT,
                MessageStatus.DELIVERED,
                deliveredAt
        );

        if (updatedCount > 0) {
            applicationEventPublisher.publishEvent(
                    new MessageDeliveredDomainEvent(
                            conversationId,
                            currentUser.getId(),
                            message.getSender().getId(),
                            message.getId(),
                            deliveredAt
                    )
            );
        }
    }

    private void validateRequest(String name, Long messageId) {
        if (name == null) {
            throw new ValidationException("Username must not be null");
        }

        if (messageId == null) {
            throw new ValidationException("MessageId must not be null");
        }
    }
}
