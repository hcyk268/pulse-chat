package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.conversation.model.ConversationType;

import java.time.Instant;
import java.util.List;

public record GroupConversationResponse(
        Long id,
        ConversationType type,
        List<ConversationParticipantResponse> participants,
        long unreadCount,
        ConversationLastMessageResponse lastMessage,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
