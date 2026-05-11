package backend.xxx.chat.conversation.dto;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.conversation.model.ConversationType;

public record DirectConversationResponse(
        Long id,
        ConversationType type,
        List<ConversationParticipantResponse> participants,
        ConversationUserResponse otherParticipant,
        long unreadCount,
        ConversationLastMessageResponse lastMessage,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
