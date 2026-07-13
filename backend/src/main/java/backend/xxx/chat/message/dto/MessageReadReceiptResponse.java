package backend.xxx.chat.message.dto;

import java.time.Instant;

import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessageReadReceiptResponse(
        SummarizeUserResponse user,
        Instant readAt
) {
}