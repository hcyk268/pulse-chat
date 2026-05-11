package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.conversation.model.ConversationType;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        Long id,
        ConversationType type,
        ConversationUserResponse otherParticipant,
        long unreadCount,
        ConversationLastMessageResponse lastMessage,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
