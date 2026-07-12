package backend.xxx.chat.conversation.dto;

import java.util.List;

import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.model.ParticipantRole;

public record ConversationDetailResponse(
        Long id,
        ConversationType type,
        String title,
        String avatarUrl,
        ConversationUserResponse peer,
        ConversationUserResponse createdBy,
        ParticipantRole currentUserRole,
        List<ConversationMemberResponse> participants,
        int participantCount,
        ConversationLastMessageResponse lastMessage,
        long unreadCount
) {
}