package backend.xxx.chat.conversation.dto;

import java.time.Instant;

import backend.xxx.chat.message.model.MessageStatus;

public record ConversationLastMessageResponse(
        Long id,
        Long senderId,
        String contentPreview,
        MessageStatus status,
        Instant createdAt
) {
}
