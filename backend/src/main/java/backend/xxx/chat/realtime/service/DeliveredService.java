package backend.xxx.chat.realtime.service;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.service.MessageAccessPolicy;
import backend.xxx.chat.message.service.MessageValidator;
import backend.xxx.chat.outbox.payload.MessageDeliveredOutboxPayload;
import backend.xxx.chat.outbox.service.OutBoxService;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveredService {

    private final UserLookupService userLookupService;
    private final MessageRepository messageRepository;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final MessageValidator messageValidator;
    private final MessageAccessPolicy messageAccessPolicy;
    private final OutBoxService outBoxService;

    @Transactional
    public void messageDelivered(String currentUsername, Long messageId) {
        messageValidator.validateDeliveredRequest(currentUsername, messageId);

        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        Long conversationId = message.getConversation().getId();
        List<ConversationParticipant> participants =
                conversationAccessPolicy.requireParticipants(conversationId);
        conversationAccessPolicy.assertCanUpdateMessageStatus(currentUser, participants);

        messageAccessPolicy.requireNotSender(message, currentUser, "Sender cannot mark their own message as delivered");

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
            outBoxService.pushEvent(
                    "MESSAGE",
                    message.getId(),
                    RealtimeEventType.MESSAGE_STATUS_UPDATED.getValue(),
                    new MessageDeliveredOutboxPayload(
                            conversationId,
                            currentUser.getId(),
                            message.getSender().getId(),
                            message.getId(),
                            deliveredAt)
            );
        }
    }
}

