package backend.xxx.chat.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.util.CursorCodec;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.dto.ConversationPinnedMessagesResponse;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.dto.*;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.message.model.MessageRead;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageReadRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.strategy.MessageTypeStrategy;
import backend.xxx.chat.message.strategy.MessageTypeStrategyRegistry;
import backend.xxx.chat.outbox.payload.MessageCreatedOutboxPayload;
import backend.xxx.chat.outbox.payload.MessageOutboxPayload;
import backend.xxx.chat.outbox.payload.MessagePinnedOutboxPayload;
import backend.xxx.chat.outbox.payload.MessageReadOutboxPayload;
import backend.xxx.chat.outbox.payload.MessageUnPinnedOutboxPayload;
import backend.xxx.chat.outbox.service.OutBoxService;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final MessageReadRepository messageReadRepository;
    private final CursorCodec cursorCodec;
    private final UserLookupService userLookupService;
    private final ConversationRepository conversationRepository;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final MessageMapper messageMapper;
    private final MessagePinMapper messagePinMapper;
    private final MessageValidator messageValidator;
    private final MessageAccessPolicy messageAccessPolicy;
    private final MessageTypeStrategyRegistry messageTypeStrategyRegistry;
    private final OutBoxService outBoxService;

    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(String currentUsername, Long conversationId, Short limit, String cursor) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(
                        () -> new NotFoundException("conversation.not.found")
                );

        conversationAccessPolicy.requireActiveParticipant(conversation.getId(), currentUser.getId());

        int pageLimit = messageValidator.normalizeLimit(
                limit,
                DEFAULT_MESSAGE_LIMIT,
                MAX_MESSAGE_LIMIT
        );
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
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));

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

        conversationAccessPolicy.filterActiveParticipants(participants).forEach(participant -> {
            participant.markVisibleInList();
            if (!participant.getUser().getId().equals(currentUser.getId())) {
                participant.incrementUnreadCount();
            }
        });

        outBoxService.pushEvent(
                "MESSAGE",
                savedMessage.getId(),
                RealtimeEventType.MESSAGE_CREATED.getValue(),
                new MessageCreatedOutboxPayload(conversation.getId(), savedMessage.getId())
        );

        return messageMapper.toResponse(savedMessage);
    }


    @Transactional(readOnly = true)
    public MessageReadReceiptsResponse getReadReceipts(String currentUsername, Long messageId) {
        messageValidator.validateMessageId(messageId);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        conversationAccessPolicy.requireActiveParticipant(message.getConversation().getId(), currentUser.getId());

        return messageMapper.toReadReceiptsResponse(
                message.getId(),
                messageReadRepository.findByMessageIdWithUserOrderByReadAtAsc(message.getId())
        );
    }
    @Transactional
    public PinMessageResult pinMessage(String currentUsername, Long messageId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireActiveParticipant(conversationId, currentUser.getId());

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));

        MessagePin existingPin = messagePinRepository.findByMessageIdWithDetails(message.getId())
                .orElse(null);
        if (existingPin != null) {
            return new PinMessageResult(messagePinMapper.toResponse(existingPin), false);
        }

        long pinnedCount = messagePinRepository.countByConversationId(conversationId);
        messageValidator.validateCanPinMessage(message, pinnedCount, MAX_PINS_PER_CONVERSATION);

        MessagePin savedPin = messagePinRepository.save(
                MessagePin.create(conversation, message, currentUser, Instant.now())
        );

        outBoxService.pushEvent(
                "MESSAGE_PIN",
                savedPin.getId(),
                RealtimeEventType.MESSAGE_PINNED.getValue(),
                new MessagePinnedOutboxPayload(conversationId, savedPin.getId())
        );

        return new PinMessageResult(messagePinMapper.toResponse(savedPin), true);
    }

    @Transactional(readOnly = true)
    public ConversationPinnedMessagesResponse getPinnedMessages(String currentUsername, Long conversationId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        conversationAccessPolicy.requireActiveParticipant(conversationId, currentUser.getId());

        List<MessagePinResponse> items = messagePinRepository.findByConversationIdWithDetails(conversationId)
                .stream()
                .map(messagePinMapper::toResponse)
                .toList();

        return new ConversationPinnedMessagesResponse(conversationId, items);
    }

    @Transactional
    public MarkReadResponse readMessage(String currentUsername, MarkReadRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));

        ConversationParticipant currentParticipant =
                conversationAccessPolicy.requireActiveParticipant(conversation.getId(), currentUser.getId());

        Message lastReadMessage = messageRepository.findByIdAndConversationId(
                        request.lastReadMessageId(),
                        conversation.getId()
                )
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        Instant readAt = Instant.now();

        saveReadReceipts(conversation.getId(), currentUser, lastReadMessage, readAt);

        if (conversation.getType() == ConversationType.DIRECT) {
            messageRepository.markMessagesReadUpTo(
                    conversation.getId(),
                    currentUser.getId(),
                    lastReadMessage.getCreatedAt(),
                    lastReadMessage.getId(),
                    MessageStatus.READ,
                    readAt
            );
        }

        currentParticipant.markRead(lastReadMessage.getId());

        outBoxService.pushEvent(
                "MESSAGE",
                lastReadMessage.getId(),
                RealtimeEventType.MESSAGE_READ.getValue(),
                new MessageReadOutboxPayload(
                        conversation.getId(),
                        currentUser.getId(),
                        lastReadMessage.getId(),
                        readAt
                )
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
                .orElseThrow(() -> new NotFoundException("message.pin.not.found"));

        Long conversationId = unPinMessage.getConversation().getId();
        conversationAccessPolicy.requireActiveParticipant(conversationId, currentUser.getId());

        Long unPinnedMessageId = unPinMessage.getMessage().getId();
        Instant unPinnedAt = Instant.now();
        messagePinRepository.delete(unPinMessage);

        outBoxService.pushEvent(
                "MESSAGE",
                unPinnedMessageId,
                RealtimeEventType.MESSAGE_UNPINNED.getValue(),
                new MessageUnPinnedOutboxPayload(conversationId, unPinnedMessageId, unPinnedAt)
        );

        return messagePinMapper.toUnPinMessageResponse(conversationId, unPinnedMessageId, unPinnedAt);
    }

    @Transactional
    public MessageResponse editMessage(String currentUsername, Long messageId, EditMessageRequest request) {
        messageValidator.validateEditMessageRequest(messageId, request);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireActiveParticipant(conversationId, currentUser.getId());

        messageAccessPolicy.requireSender(message, currentUser, "Only message sender can edit this message");
        messageValidator.validateCanEditMessage(message, request);

        message.editContent(request.newContent(), Instant.now());

        outBoxService.pushEvent(
                "MESSAGE",
                message.getId(),
                RealtimeEventType.MESSAGE_UPDATED.getValue(),
                new MessageOutboxPayload(conversationId, message.getId())
        );

        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse deleteMessage(String currentUsername, Long messageId) {
        messageValidator.validateMessageId(messageId);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new NotFoundException("message.not.found"));

        Long conversationId = message.getConversation().getId();
        conversationAccessPolicy.requireActiveParticipant(conversationId, currentUser.getId());

        messageAccessPolicy.requireSender(message, currentUser, "Only message sender can delete this message");
        messageValidator.validateCanDeleteMessage(message);

        if (!message.isDeleted()) {
            message.deleteForEveryone(currentUser, Instant.now());

            outBoxService.pushEvent(
                    "MESSAGE",
                    message.getId(),
                    RealtimeEventType.MESSAGE_DELETED.getValue(),
                    new MessageOutboxPayload(conversationId, message.getId())
            );
        }

        return messageMapper.toResponse(message);
    }


    private void saveReadReceipts(Long conversationId, User reader, Message lastReadMessage, Instant readAt) {
        List<Message> unreadMessages = messageReadRepository.findUnreadMessagesByReaderUpTo(
                conversationId,
                reader.getId(),
                lastReadMessage.getCreatedAt(),
                lastReadMessage.getId()
        );

        if (unreadMessages.isEmpty()) {
            return;
        }

        messageReadRepository.saveAll(unreadMessages.stream()
                .map(message -> MessageRead.create(message, reader, readAt))
                .toList());
    }
    private Message resolveReplyToMessage(Long replyToMessageId, Long conversationId) {
        if (replyToMessageId == null) {
            return null;
        }

        Message replyToMessage = messageRepository.findByIdWithConversationAndSender(replyToMessageId)
                .orElseThrow(() -> new NotFoundException("message.reply.not.found"));

        messageValidator.validateReplyToMessage(replyToMessage, conversationId);

        return replyToMessage;
    }

    private MessageCursor decodeCursor(String cursor) {
        MessageCursor messageCursor = cursorCodec.decode(cursor, MessageCursor.class, "message.cursor.invalid");

        messageValidator.validateMessageCursor(messageCursor);

        return messageCursor;
    }

    private String encodeCursor(MessageCursor messageCursor) {
        return cursorCodec.encode(messageCursor, "message.cursor.build.failed");
    }

    public record PinMessageResult(
            MessagePinResponse response,
            boolean created
    ) {
    }
}

