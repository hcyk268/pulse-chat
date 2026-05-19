package backend.xxx.chat.message.dto;

import java.time.Instant;

import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessageReplyResponse(
        Long id,
        SummarizeUserResponse sender,
        String content,
        MessageType messageType,
        Instant createdAt,
        Instant deletedAt
) {
}
