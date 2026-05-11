package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.user.dto.PresenceResponse;

public record ConversationUserResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        PresenceResponse presence
) {
}
