package backend.xxx.chat.message.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.MarkReadRequest;
import backend.xxx.chat.message.dto.MarkReadResponse;
import backend.xxx.chat.message.dto.MessageCursor;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.realtime.event.MessageCreatedDomainEvent;
import backend.xxx.chat.realtime.event.MessageReadDomainEvent;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final UserLookupService userLookupService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageMapper messageMapper;
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

        if (!conversationParticipantRepository.existsById(
                new ConversationParticipantId(conversation.getId(), currentUser.getId())
        )) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "You are not allowed to access this conversation"
            );
        }

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
                conversationParticipantRepository.findByConversationIdWithUser(conversation.getId());

        boolean isCurrentUserParticipant = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(currentUser.getId()));

        if (!isCurrentUserParticipant) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "You are not allowed to send message to this conversation"
            );
        }

        Message existingMessage = messageRepository.findByConversationIdAndClientMessageIdWithSender(
                        conversation.getId(),
                        request.clientMessageId()
                )
                .orElse(null);

        if (existingMessage != null) {
            return messageMapper.toResponse(existingMessage);
        }

        if (request.messageType() != MessageType.TEXT) {
            throw new ValidationException("Only TEXT messages are supported");
        }

        Message message = Message.createTextMessage(
                conversation,
                currentUser,
                request.clientMessageId(),
                request.content()
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
    public MarkReadResponse readMessage(String currentUsername, MarkReadRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        ConversationParticipant currentParticipant = conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversation.getId(), currentUser.getId())
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "You are not allowed to read this conversation"
                ));

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

    private int normalizeLimit(Short limit) {
        int pageLimit = limit == null ? DEFAULT_MESSAGE_LIMIT : limit;

        if (pageLimit < 1 || pageLimit > MAX_MESSAGE_LIMIT) {
            throw new ValidationException("limit must be between 1 and " + MAX_MESSAGE_LIMIT);
        }

        return pageLimit;
    }

    private MessageCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String json = new String(
                    Base64.getDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            MessageCursor messageCursor = objectMapper.readValue(json, MessageCursor.class);
            if (messageCursor.createdAt() == null || messageCursor.messageId() == null) {
                throw new ValidationException("Invalid message cursor");
            }
            return messageCursor;
        } catch (ValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ValidationException("Invalid message cursor");
        }
    }

    private String encodeCursor(MessageCursor messageCursor) {
        if (messageCursor == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(messageCursor);
            return Base64.getEncoder()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to build message cursor"
            );
        }
    }
}
