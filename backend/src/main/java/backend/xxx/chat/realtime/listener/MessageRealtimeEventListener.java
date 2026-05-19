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
import backend.xxx.chat.realtime.event.*;
import backend.xxx.chat.realtime.model.*;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public void onMessageUpdated(MessageUpdatedDomainEvent event) {
        Message message = messageRepository.findByIdWithConversationAndSender(event.messageId())
                .orElseThrow();
        MessageUpdatedEventData messageData = new MessageUpdatedEventData(messageMapper.toResponse(message));

        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        Map<String, ConversationResponse> conversationByUsername = Objects.equals(
                message.getConversation().getLastMessageId(),
                message.getId()
        )
                ? conversationResponseBuilder.buildByUsernameForParticipants(participants)
                : Map.of();

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.MESSAGE_UPDATED,
                    event.conversationId(),
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    event.conversationId(),
                    new ConversationUpdatedEventData(conversationResponse)
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessageDeleted(MessageDeletedDomainEvent event) {
        Message message = messageRepository.findByIdWithConversationAndSender(event.messageId())
                .orElseThrow();
        MessageDeletedEventData messageData = new MessageDeletedEventData(messageMapper.toResponse(message));

        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        Map<String, ConversationResponse> conversationByUsername = Objects.equals(
                message.getConversation().getLastMessageId(),
                message.getId()
        )
                ? conversationResponseBuilder.buildByUsernameForParticipants(participants)
                : Map.of();

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.MESSAGE_DELETED,
                    event.conversationId(),
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            realtimeEventPublisher.sendToUser(
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    event.conversationId(),
                    new ConversationUpdatedEventData(conversationResponse)
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

        sendToConversationParticipants(participants, event.conversationId(), RealtimeEventType.MESSAGE_PINNED, data);
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

        sendToConversationParticipants(participants, event.conversationId(), RealtimeEventType.MESSAGE_READ, data);
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

        sendToConversationParticipants(
                List.of(senderParticipant),
                event.conversationId(),
                RealtimeEventType.MESSAGE_STATUS_UPDATED,
                data
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onMessageUnPinned(MessageUnPinnedDomainEvent event) {
        MessageUnPinnedEventData data = new MessageUnPinnedEventData(event.messageId(), event.unPinnedAt());

        List<ConversationParticipant> participants =
                participantRepository.findByConversationIdWithUser(event.conversationId());

        sendToConversationParticipants(participants, event.conversationId(), RealtimeEventType.MESSAGE_UNPINNED, data);
    }

    private void sendToConversationParticipants(
            List<ConversationParticipant> participants,
            Long conversationId,
            RealtimeEventType eventType,
            Object data
    ) {
        for (ConversationParticipant participant : participants) {
            realtimeEventPublisher.sendToUser(
                    participant.getUser().getUsername(),
                    eventType,
                    conversationId,
                    data
            );
        }
    }
}
