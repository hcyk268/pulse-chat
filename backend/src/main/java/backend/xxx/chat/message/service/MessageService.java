package backend.xxx.chat.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.util.CursorCodec;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.dto.ConversationPinsResponse;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.dto.*;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.strategy.MessageTypeStrategy;
import backend.xxx.chat.message.strategy.MessageTypeStrategyRegistry;
import backend.xxx.chat.realtime.event.MessageCreatedDomainEvent;
import backend.xxx.chat.realtime.event.MessageDeletedDomainEvent;
import backend.xxx.chat.realtime.event.MessagePinnedDomainEvent;
import backend.xxx.chat.realtime.event.MessageReadDomainEvent;
import backend.xxx.chat.realtime.event.MessageUnPinnedDomainEvent;
import backend.xxx.chat.realtime.event.MessageUpdatedDomainEvent;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int DEFAULT_MESSAGE_LIMIT = 20;
    private static final int MAX_MESSAGE_LIMIT = 50;
    private static final int MAX_PINS_PER_CONVERSATION = 20;

    private final MessageRepository messageRepository;
    private final MessagePinRepository messagePinRepository;
    private final CursorCodec cursorCodec;
    private final UserLookupService userLookupService;
    private final ConversationRepository conversationRepository;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final MessageMapper messageMapper;
    private final MessagePinMapper messagePinMapper;
    private final MessageTypeStrategyRegistry messageTypeStrategyRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(String currentUsername, Long conversationId, Short limit, String cursor) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(
                        () -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                ErrorCode.NOT_FOUND,
                                "Conversation not found"
                        )
                );

        conversationAccessPolicy.requireParticipant(conversation.getId(), currentUser.getId());

        int pageLimit = normalizeLimit(limit);
        MessageCursor messageCursor = decodeCursor(cursor);

        PageRequest pageRequest = PageRequest.of(0, pageLimit + 1);
        List<Message> messages = messageCursor == null
                ? messageRepository.findFirstPageByConversationId(
                        conversation.getId(),
                        pageRequest
                )
                : messageRepository.findPageByConversationIdAfterCursor(
                        conversation.getId(),
                        messageCursor.createdAt(),
                        messageCursor.messageId(),
                        pageRequest
                );

        boolean hasMore = messages.size() > pageLimit;
        List<Message> page = hasMore ? messages.subList(0, pageLimit) : messages;
        List<Message> displayPage = new ArrayList<>(page);
        Collections.reverse(displayPage);

        String nextCursor = null;
        if (hasMore) {
            Message lastMessage = page.get(page.size() - 1);
            nextCursor = encodeCursor(new MessageCursor(lastMessage.getCreatedAt(), lastMessage.getId()));
        }

        List<MessageResponse> items = displayPage.stream()
                .map(messageMapper::toResponse)
                .toList();

        return new MessageHistoryResponse(
                conversation.getId(),
                items,
                new CursorPageResponse(pageLimit, nextCursor, hasMore, null)
        );
    }

    @Transactional
    public MessageResponse sendMessage(String currentUsername, SendMessageRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        List<ConversationParticipant> participants =
                conversationAccessPolicy.requireParticipants(conversation.getId());
        conversationAccessPolicy.assertCanSendMessage(currentUser, participants);

        Message existingMessage = messageRepository.findByConversationIdAndClientMessageIdWithSender(
                        conversation.getId(),
                        request.clientMessageId()
                )
                .orElse(null);

        if (existingMessage != null) {
            return messageMapper.toResponse(existingMessage);
        }

        Message replyToMessage = resolveReplyToMessage(request.replyToMessageId(), conversation.getId());

        MessageTypeStrategy strategy = messageTypeStrategyRegistry.get(request.messageType());
        Message message = strategy.createMessage(
                conversation,
                currentUser,
                request,
                replyToMessage
        );
        Message savedMessage = messageRepository.save(message);

        conversation.updateLastMessage(savedMessage.getId(), savedMessage.getCreatedAt());

        participants.forEach(participant -> {
            participant.markVisibleInList();
            if (!participant.getUser().getId().equals(currentUser.getId())) {
                participant.incrementUnreadCount();
            }
        });

        applicationEventPublisher.publishEvent(
                new MessageCreatedDomainEvent(conversation.getId(), savedMessage.getId())
        );

        return messageMapper.toResponse(savedMessage);
    }

    @Transactional
    public PinMessageResult pinMessage(String currentUsername, Long messageId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        MessagePin existingPin = messagePinRepository.findByMessageIdWithDetails(message.getId())
                .orElse(null);
        if (existingPin != null) {
            return new PinMessageResult(messagePinMapper.toResponse(existingPin), false);
        }

        if (message.isDeleted()) {
            throw new ValidationException("Deleted message cannot be pinned");
        }

        long pinnedCount = messagePinRepository.countByConversationId(conversationId);
        if (pinnedCount >= MAX_PINS_PER_CONVERSATION) {
            throw new ConflictException("Conversation can only have 20 pinned messages");
        }

        MessagePin savedPin = messagePinRepository.save(
                MessagePin.create(conversation, message, currentUser, Instant.now())
        );

        applicationEventPublisher.publishEvent(
                new MessagePinnedDomainEvent(conversationId, savedPin.getId())
        );

        return new PinMessageResult(messagePinMapper.toResponse(savedPin), true);
    }

    @Transactional(readOnly = true)
    public ConversationPinsResponse getConversationPins(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        List<MessagePinResponse> items = messagePinRepository.findByConversationIdWithDetails(conversationId)
                .stream()
                .map(messagePinMapper::toResponse)
                .toList();

        return new ConversationPinsResponse(conversationId, items);
    }

    @Transactional
    public MarkReadResponse readMessage(String currentUsername, MarkReadRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        ConversationParticipant currentParticipant =
                conversationAccessPolicy.requireParticipant(conversation.getId(), currentUser.getId());

        Message lastReadMessage = messageRepository.findByIdAndConversationId(
                        request.lastReadMessageId(),
                        conversation.getId()
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        Instant readAt = Instant.now();

        messageRepository.markMessagesReadUpTo(
                conversation.getId(),
                currentUser.getId(),
                lastReadMessage.getCreatedAt(),
                lastReadMessage.getId(),
                MessageStatus.READ,
                readAt
        );

        currentParticipant.markRead(lastReadMessage.getId());

        applicationEventPublisher.publishEvent(
                new MessageReadDomainEvent(
                        conversation.getId() ,
                        currentUser.getId(),
                        lastReadMessage.getId(),
                        readAt)
        );

        return new MarkReadResponse(
                conversation.getId(),
                lastReadMessage.getId(),
                readAt,
                currentParticipant.getUnreadCount()
        );
    }

    @Transactional
    public UnPinMessageResponse unPinMessage(String currentUsername, Long messageId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        MessagePin unPinMessage = messagePinRepository.findByMessageIdWithDetails(messageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message is not pinned before"
                ));

        Long conversationId = unPinMessage.getConversation().getId();
        conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        Long unPinnedMessageId = unPinMessage.getMessage().getId();
        Instant unPinnedAt = Instant.now();
        messagePinRepository.delete(unPinMessage);

        applicationEventPublisher.publishEvent(
                new MessageUnPinnedDomainEvent(conversationId, unPinnedMessageId, unPinnedAt)
        );

        return messagePinMapper.toUnPinMessageResponse(conversationId, unPinnedMessageId, unPinnedAt);
    }

    @Transactional
    public MessageResponse editMessage(String currentUsername, Long messageId, EditMessageRequest request) {
        validateEditMessageRequest(messageId, request);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Only message sender can edit this message"
            );
        }

        if (message.isDeleted()) {
            throw new ValidationException("Deleted message cannot be edited");
        }

        if (request.type() != null && request.type() != message.getMessageType()) {
            throw new ValidationException("message type cannot be changed");
        }

        if (message.getMessageType() != MessageType.TEXT) {
            throw new ValidationException("Only text message can be edited");
        }

        message.editContent(request.newContent(), Instant.now());

        applicationEventPublisher.publishEvent(
                new MessageUpdatedDomainEvent(conversationId, message.getId())
        );

        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse deleteMessage(String currentUsername, Long messageId) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Message not found"
                ));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireParticipant(conversationId, currentUser.getId());

        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Only message sender can delete this message"
            );
        }

        if (!message.isDeleted()) {
            message.deleteForEveryone(currentUser, Instant.now());

            applicationEventPublisher.publishEvent(
                    new MessageDeletedDomainEvent(conversationId, message.getId())
            );
        }

        return messageMapper.toResponse(message);
    }

    private Message resolveReplyToMessage(Long replyToMessageId, Long conversationId) {
        if (replyToMessageId == null) {
            return null;
        }

        Message replyToMessage = messageRepository.findByIdWithConversationAndSender(replyToMessageId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Reply message not found"
                ));

        if (!replyToMessage.getConversation().getId().equals(conversationId)) {
            throw new ValidationException("Reply message must belong to the same conversation");
        }

        if (replyToMessage.isDeleted()) {
            throw new ValidationException("Deleted message cannot be replied");
        }

        return replyToMessage;
    }

    private void validateEditMessageRequest(Long messageId, EditMessageRequest request) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }

        if (request == null || request.newContent() == null || request.newContent().trim().isEmpty()) {
            throw new ValidationException("newContent must not be blank");
        }
    }

    private int normalizeLimit(Short limit) {
        int pageLimit = limit == null ? DEFAULT_MESSAGE_LIMIT : limit;

        if (pageLimit < 1 || pageLimit > MAX_MESSAGE_LIMIT) {
            throw new ValidationException("limit must be between 1 and " + MAX_MESSAGE_LIMIT);
        }

        return pageLimit;
    }

    private MessageCursor decodeCursor(String cursor) {
        MessageCursor messageCursor = cursorCodec.decode(cursor, MessageCursor.class, "Invalid message cursor");

        if (messageCursor != null && (messageCursor.createdAt() == null || messageCursor.messageId() == null)) {
            throw new ValidationException("Invalid message cursor");
        }

        return messageCursor;
    }

    private String encodeCursor(MessageCursor messageCursor) {
        return cursorCodec.encode(messageCursor, "Failed to build message cursor");
    }

    public record PinMessageResult(
            MessagePinResponse response,
            boolean created
    ) {
    }
}
