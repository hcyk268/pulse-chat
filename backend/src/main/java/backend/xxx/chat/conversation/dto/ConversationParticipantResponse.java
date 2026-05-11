package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.user.dto.PresenceResponse;

public record ConversationParticipantResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        PresenceResponse presence,
        boolean isVisibleInList
) {
}
