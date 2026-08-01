package backend.xxx.chat.realtime.service;

import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.conversation.service.ConversationResponseBuilder;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessageAttachmentRepository;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.service.MessageMapper;
import backend.xxx.chat.message.service.MessagePinMapper;
import backend.xxx.chat.realtime.model.ConversationUpdatedEventData;
import backend.xxx.chat.realtime.model.MessageCreatedEventData;
import backend.xxx.chat.realtime.model.MessageDeletedEventData;
import backend.xxx.chat.realtime.model.MessageDeliveredEventData;
import backend.xxx.chat.realtime.model.MessagePinnedEventData;
import backend.xxx.chat.realtime.model.MessageReadEventData;
import backend.xxx.chat.realtime.model.MessageUnPinnedEventData;
import backend.xxx.chat.realtime.model.MessageUpdatedEventData;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageRealtimeNotifier {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessagePinRepository messagePinRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageMapper messageMapper;
    private final MessagePinMapper messagePinMapper;
    private final ConversationResponseBuilder conversationResponseBuilder;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Transactional(readOnly = true)
    public void notifyCreated(Long outboxEventId, Long conversationId, Long messageId) {
        Message message = messageRepository.findByIdInWithSender(List.of(messageId))
                .stream()
                .findFirst()
                .orElseThrow();

        Map<Long, List<MessageAttachment>> attachmentsByMessageId =
                findAttachmentsByMessageId(collectMessageAndReplyIds(List.of(message)));
        MessageResponse messageResponse = messageMapper.toResponse(message, attachmentsByMessageId);
        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);
        Map<String, ConversationResponse> conversationByUsername =
                conversationResponseBuilder.buildByUsernameForParticipants(
                        participants,
                        Map.of(message.getId(), message),
                        attachmentsByMessageId
                );

        MessageCreatedEventData messageData = new MessageCreatedEventData(messageResponse);

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.MESSAGE_CREATED),
                    username,
                    RealtimeEventType.MESSAGE_CREATED,
                    conversationId,
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.CONVERSATION_UPDATED),
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    conversationId,
                    new ConversationUpdatedEventData(conversationResponse)
            );
        }
    }

    @Transactional(readOnly = true)
    public void notifyUpdated(Long outboxEventId, Long conversationId, Long messageId) {
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow();
        Map<Long, List<MessageAttachment>> attachmentsByMessageId =
                findAttachmentsByMessageId(collectMessageAndReplyIds(List.of(message)));
        MessageUpdatedEventData messageData = new MessageUpdatedEventData(
                messageMapper.toResponse(message, attachmentsByMessageId)
        );

        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        Map<String, ConversationResponse> conversationByUsername = Objects.equals(
                message.getConversation().getLastMessageId(),
                message.getId()
        )
                ? conversationResponseBuilder.buildByUsernameForParticipants(
                        participants,
                        Map.of(message.getId(), message),
                        attachmentsByMessageId
                )
                : Map.of();

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.MESSAGE_UPDATED),
                    username,
                    RealtimeEventType.MESSAGE_UPDATED,
                    conversationId,
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.CONVERSATION_UPDATED),
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    conversationId,
                    new ConversationUpdatedEventData(conversationResponse)
            );
        }
    }

    @Transactional(readOnly = true)
    public void notifyDeleted(Long outboxEventId, Long conversationId, Long messageId) {
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow();
        Map<Long, List<MessageAttachment>> attachmentsByMessageId =
                findAttachmentsByMessageId(collectMessageAndReplyIds(List.of(message)));
        MessageDeletedEventData messageData = new MessageDeletedEventData(
                messageMapper.toResponse(message, attachmentsByMessageId)
        );

        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        Map<String, ConversationResponse> conversationByUsername = Objects.equals(
                message.getConversation().getLastMessageId(),
                message.getId()
        )
                ? conversationResponseBuilder.buildByUsernameForParticipants(
                        participants,
                        Map.of(message.getId(), message),
                        attachmentsByMessageId
                )
                : Map.of();

        for (ConversationParticipant participant : participants) {
            String username = participant.getUser().getUsername();

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.MESSAGE_DELETED),
                    username,
                    RealtimeEventType.MESSAGE_DELETED,
                    conversationId,
                    messageData
            );

            ConversationResponse conversationResponse = conversationByUsername.get(username);
            if (conversationResponse == null) {
                continue;
            }

            realtimeEventPublisher.sendToUser(
                    buildOutboxEventId(outboxEventId, RealtimeEventType.CONVERSATION_UPDATED),
                    username,
                    RealtimeEventType.CONVERSATION_UPDATED,
                    conversationId,
                    new ConversationUpdatedEventData(conversationResponse)
            );
        }
    }

    @Transactional(readOnly = true)
    public void notifyPinned(Long outboxEventId, Long conversationId, Long messagePinId) {
        MessagePin messagePin = messagePinRepository.findByIdWithDetails(messagePinId)
                .orElseThrow();
        Map<Long, List<MessageAttachment>> attachmentsByMessageId =
                findAttachmentsByMessageId(collectMessageAndReplyIds(List.of(messagePin.getMessage())));
        MessagePinnedEventData data = new MessagePinnedEventData(
                messagePinMapper.toResponse(messagePin, attachmentsByMessageId)
        );

        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        sendToConversationParticipants(outboxEventId, participants, conversationId, RealtimeEventType.MESSAGE_PINNED, data);
    }

    @Transactional(readOnly = true)
    public void notifyRead(Long outboxEventId, Long conversationId, Long readerId, Long lastReadMessageId, Instant readAt) {
        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        ConversationParticipant readParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(readerId))
                .findFirst()
                .orElse(null);
        if (readParticipant == null) {
            return;
        }

        MessageReadEventData data = new MessageReadEventData(
                readerId,
                readParticipant.getUser().getUsername(),
                lastReadMessageId,
                readAt
        );

        sendToConversationParticipants(outboxEventId, participants, conversationId, RealtimeEventType.MESSAGE_READ, data);
    }

    @Transactional(readOnly = true)
    public void notifyDelivered(Long outboxEventId, Long conversationId, Long senderId, Long lastDeliveredMessageId, Instant deliveredAt) {
        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        ConversationParticipant senderParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(senderId))
                .findFirst()
                .orElse(null);
        if (senderParticipant == null) {
            return;
        }

        MessageDeliveredEventData data = new MessageDeliveredEventData(
                lastDeliveredMessageId,
                MessageStatus.DELIVERED,
                deliveredAt
        );

        sendToConversationParticipants(outboxEventId, List.of(senderParticipant), conversationId, RealtimeEventType.MESSAGE_STATUS_UPDATED, data);
    }

    @Transactional(readOnly = true)
    public void notifyUnPinned(Long outboxEventId, Long conversationId, Long messageId, Instant unPinnedAt) {
        MessageUnPinnedEventData data = new MessageUnPinnedEventData(messageId, unPinnedAt);

        List<ConversationParticipant> participants =
                findActiveParticipants(conversationId);

        sendToConversationParticipants(outboxEventId, participants, conversationId, RealtimeEventType.MESSAGE_UNPINNED, data);
    }


    private Set<Long> collectMessageAndReplyIds(Collection<Message> messages) {
        Set<Long> messageIds = messages.stream()
                .map(Message::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        messages.stream()
                .map(Message::getReplyToMessage)
                .filter(Objects::nonNull)
                .map(Message::getId)
                .forEach(messageIds::add);
        return messageIds;
    }

    private Map<Long, List<MessageAttachment>> findAttachmentsByMessageId(Collection<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return messageAttachmentRepository.findByMessageIdInWithUploadedAsset(messageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        attachment -> attachment.getMessage().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<ConversationParticipant> findActiveParticipants(Long conversationId) {
        return conversationAccessPolicy.filterActiveParticipants(
                participantRepository.findByConversationIdWithUser(conversationId)
        );
    }

    private void sendToConversationParticipants(
            Long outboxEventId,
            List<ConversationParticipant> participants,
            Long conversationId,
            RealtimeEventType eventType,
            Object data
    ) {
        String realtimeEventId = buildOutboxEventId(outboxEventId, eventType);

        for (ConversationParticipant participant : participants) {
            realtimeEventPublisher.sendToUser(
                    realtimeEventId,
                    participant.getUser().getUsername(),
                    eventType,
                    conversationId,
                    data
            );
        }
    }

    private String buildOutboxEventId(Long outboxEventId, RealtimeEventType eventType) {
        if (outboxEventId == null) {
            return null;
        }

        return "outbox_" + outboxEventId + "_" + eventType.getValue();
    }
}
