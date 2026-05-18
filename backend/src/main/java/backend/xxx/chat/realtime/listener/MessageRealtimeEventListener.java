package backend.xxx.chat.realtime.listener;

import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.service.ConversationResponseBuilder;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.service.MessageMapper;
import backend.xxx.chat.message.service.MessagePinMapper;
import backend.xxx.chat.realtime.event.MessageCreatedDomainEvent;
import backend.xxx.chat.realtime.event.MessageDeliveredDomainEvent;
import backend.xxx.chat.realtime.event.MessagePinnedDomainEvent;
import backend.xxx.chat.realtime.event.MessageReadDomainEvent;
import backend.xxx.chat.realtime.model.ConversationUpdatedEventData;
import backend.xxx.chat.realtime.model.MessageCreatedEventData;
import backend.xxx.chat.realtime.model.MessageDeliveredEventData;
import backend.xxx.chat.realtime.model.MessagePinnedEventData;
import backend.xxx.chat.realtime.model.MessageReadEventData;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageRealtimeEventListener {

    private final MessageRepository messageRepository;
    private final MessagePinRepository messagePinRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageMapper messageMapper;
    private final MessagePinMapper messagePinMapper;
    private final ConversationResponseBuilder conversationResponseBuilder;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessageCreated(MessageCreatedDomainEvent event) {
        Message message = messageRepository.findByIdInWithSender(List.of(event.messageId()))
                .stream()
                .findFirst()
                .orElseThrow();

        MessageResponse messageResponse = messageMapper.toResponse(message);

        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());
        Map<String, ConversationResponse> conversationByUsername =
                conversationResponseBuilder.buildByUsernameForParticipants(participants);

        MessageCreatedEventData messageData = new MessageCreatedEventData(messageResponse);

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.MESSAGE_CREATED,
                    event.conversationId(),
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            ConversationUpdatedEventData conversationData = new ConversationUpdatedEventData(conversationResponse);

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    event.conversationId(),
                    conversationData
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessagePinned(MessagePinnedDomainEvent event) {
        MessagePinnedEventData data = messagePinRepository.findByIdWithDetails(event.messagePinId())
                .map(messagePinMapper::toResponse)
                .map(MessagePinnedEventData::new)
                .orElseThrow();

        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        for (ConversationParticipant participant : participants) {
            realtimeEventPublisher.sendToUser(
                    participant.getUser().getUsername(),
                    RealtimeEventType.MESSAGE_PINNED,
                    event.conversationId(),
                    data
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessageRead(MessageReadDomainEvent event) {
        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        ConversationParticipant readParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(event.readerId()))
                .findFirst()
                .orElseThrow();

        MessageReadEventData data = new MessageReadEventData(
                event.readerId(),
                readParticipant.getUser().getUsername(),
                event.lastReadMessageId(),
                event.readAt()
        );

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.MESSAGE_READ,
                    event.conversationId(),
                    data
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessageDelivered(MessageDeliveredDomainEvent event) {
        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        ConversationParticipant senderParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(event.senderId()))
                .findFirst()
                .orElseThrow();

        MessageDeliveredEventData data = new MessageDeliveredEventData(
                event.lastDeliveredMessageId(),
                MessageStatus.DELIVERED,
                event.deliveredAt()
        );

        realtimeEventPublisher.sendToUser(
                senderParticipant.getUser().getUsername(),
                RealtimeEventType.MESSAGE_STATUS_UPDATED,
                event.conversationId(),
                data
        );
    }
}
