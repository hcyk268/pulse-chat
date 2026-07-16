package backend.xxx.chat.message.service;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.web.Translator;
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
            throw new ValidationException(Translator.toLocale("validation.limit.range", maxLimit));
        }

        return pageLimit;
    }

    public void validateMessageCursor(MessageCursor messageCursor) {
        if (messageCursor != null && (messageCursor.createdAt() == null || messageCursor.messageId() == null)) {
            throw new ValidationException("message.cursor.invalid");
        }
    }

    public void validateMessageId(Long messageId) {
        if (messageId == null) {
            throw new ValidationException("message.id.required");
        }
    }

    public void validateEditMessageRequest(Long messageId, EditMessageRequest request) {
        validateMessageId(messageId);

        if (request == null || request.newContent() == null || request.newContent().trim().isEmpty()) {
            throw new ValidationException("message.content.edit.blank");
        }
    }

    public void validateCanEditMessage(Message message, EditMessageRequest request) {
        validateNotDeleted(message, "message.deleted.cannot.edit");

        if (request.type() != null && request.type() != message.getMessageType()) {
            throw new ValidationException("message.type.change.not.allowed");
        }

        if (message.getMessageType() != MessageType.TEXT) {
            throw new ValidationException("message.text.only.editable");
        }
    }

    public void validateCanDeleteMessage(Message message) {
        validateMessageId(message == null ? null : message.getId());
    }

    public void validateCanPinMessage(Message message, long pinnedCount, int maxPinsPerConversation) {
        validateNotDeleted(message, "message.deleted.cannot.pin");

        if (pinnedCount >= maxPinsPerConversation) {
            throw new ConflictException(Translator.toLocale("conversation.pinned.limit.exceeded", maxPinsPerConversation));
        }
    }

    public void validateReplyToMessage(Message replyToMessage, Long conversationId) {
        if (!replyToMessage.getConversation().getId().equals(conversationId)) {
            throw new ValidationException("message.reply.conversation.mismatch");
        }

        validateNotDeleted(replyToMessage, "message.deleted.cannot.reply");
    }

    public void validateReactionRequest(Long messageId, MessageReactionRequest request) {
        validateMessageId(messageId);

        if (request == null || request.emoji() == null) {
            throw new ValidationException("message.reaction.emoji.required");
        }
    }

    public void validateRemoveReactionRequest(Long messageId, MessageReactionEmoji emoji) {
        validateMessageId(messageId);

        if (emoji == null) {
            throw new ValidationException("message.reaction.emoji.required");
        }
    }

    public void validateCanReact(Message message) {
        validateNotDeleted(message, "message.deleted.cannot.react");
    }

    public void validateDeliveredRequest(String username, Long messageId) {
        if (username == null) {
            throw new ValidationException("message.delivered.username.required");
        }

        validateMessageId(messageId);
    }

    private void validateNotDeleted(Message message, String messageText) {
        if (message.isDeleted()) {
            throw new ValidationException(messageText);
        }
    }
}
