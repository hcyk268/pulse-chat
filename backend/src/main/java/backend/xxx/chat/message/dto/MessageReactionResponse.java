package backend.xxx.chat.message.dto;

import java.time.Instant;

import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessageReactionResponse(
        Long messageId,
        MessageReactionEmoji emoji,
        SummarizeUserResponse reactedBy,
        Instant reactedAt
) {
}
