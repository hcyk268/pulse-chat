package backend.xxx.chat.message.dto;

import java.time.Instant;

import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessagePinResponse(
        Long id,
        Long conversationId,
        Long messageId,
        SummarizeUserResponse pinnedBy,
        Instant pinnedAt
) {
}
