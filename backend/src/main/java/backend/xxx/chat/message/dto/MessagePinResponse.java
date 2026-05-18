package backend.xxx.chat.message.dto;

import java.time.Instant;

import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessagePinResponse(
        Long pinId,
        MessageResponse message,
        SummarizeUserResponse pinnedBy,
        Instant pinnedAt
) {
}
