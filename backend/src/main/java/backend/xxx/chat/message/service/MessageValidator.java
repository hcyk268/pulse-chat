package backend.xxx.chat.message.service;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.dto.EditMessageRequest;
import backend.xxx.chat.message.dto.MessageCursor;
import backend.xxx.chat.message.dto.MessageReactionRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.message.model.MessageType;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {

    public int normalizeLimit(Short limit, int defaultLimit, int maxLimit) {
        int pageLimit = limit == null ? defaultLimit : limit;

        if (pageLimit < 1 || pageLimit > maxLimit) {
            throw new ValidationException("limit must be between 1 and " + maxLimit);
        }

        return pageLimit;
    }

    public void validateMessageCursor(MessageCursor messageCursor) {
        if (messageCursor != null && (messageCursor.createdAt() == null || messageCursor.messageId() == null)) {
            throw new ValidationException("Invalid message cursor");
        }
    }

    public void validateMessageId(Long messageId) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }
    }

    public void validateEditMessageRequest(Long messageId, EditMessageRequest request) {
        validateMessageId(messageId);

        if (request == null || request.newContent() == null || request.newContent().trim().isEmpty()) {
            throw new ValidationException("newContent must not be blank");
        }
    }

    public void validateCanEditMessage(Message message, EditMessageRequest request) {
        validateNotDeleted(message, "Deleted message cannot be edited");

        if (request.type() != null && request.type() != message.getMessageType()) {
            throw new ValidationException("message type cannot be changed");
        }

        if (message.getMessageType() != MessageType.TEXT) {
            throw new ValidationException("Only text message can be edited");
        }
    }

    public void validateCanDeleteMessage(Message message) {
        validateMessageId(message == null ? null : message.getId());
    }

    public void validateCanPinMessage(Message message, long pinnedCount, int maxPinsPerConversation) {
        validateNotDeleted(message, "Deleted message cannot be pinned");

        if (pinnedCount >= maxPinsPerConversation) {
            throw new ConflictException("Conversation can only have " + maxPinsPerConversation + " pinned messages");
        }
    }

    public void validateReplyToMessage(Message replyToMessage, Long conversationId) {
        if (!replyToMessage.getConversation().getId().equals(conversationId)) {
            throw new ValidationException("Reply message must belong to the same conversation");
        }

        validateNotDeleted(replyToMessage, "Deleted message cannot be replied");
    }

    public void validateReactionRequest(Long messageId, MessageReactionRequest request) {
        validateMessageId(messageId);

        if (request == null || request.emoji() == null) {
            throw new ValidationException("emoji must not be null");
        }
    }

    public void validateRemoveReactionRequest(Long messageId, MessageReactionEmoji emoji) {
        validateMessageId(messageId);

        if (emoji == null) {
            throw new ValidationException("emoji must not be null");
        }
    }

    public void validateCanReact(Message message) {
        validateNotDeleted(message, "Deleted message cannot be reacted");
    }

    public void validateDeliveredRequest(String username, Long messageId) {
        if (username == null) {
            throw new ValidationException("Username must not be null");
        }

        validateMessageId(messageId);
    }

    private void validateNotDeleted(Message message, String messageText) {
        if (message.isDeleted()) {
            throw new ValidationException(messageText);
        }
    }
}
