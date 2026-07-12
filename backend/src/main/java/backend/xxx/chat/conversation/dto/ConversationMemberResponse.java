package backend.xxx.chat.conversation.dto;

import java.time.Instant;

import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.conversation.model.ParticipantStatus;
import backend.xxx.chat.user.dto.PresenceResponse;

public record ConversationMemberResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        PresenceResponse presence,
        ParticipantRole role,
        Instant joinedAt,
        Instant leftAt
) {
}