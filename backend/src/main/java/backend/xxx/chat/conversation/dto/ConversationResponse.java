package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.conversation.model.ParticipantStatus;

public record ConversationResponse(
        Long id,
        ConversationType type,
        String title,
        String avatarUrl,
        ConversationUserResponse peer,
        ParticipantRole currentUserRole,
        ParticipantStatus joinStatus,
        int participantCount,
        ConversationLastMessageResponse lastMessage,
        long unreadCount
) {
    public ConversationUserResponse otherParticipant() {
        return peer;
    }
}