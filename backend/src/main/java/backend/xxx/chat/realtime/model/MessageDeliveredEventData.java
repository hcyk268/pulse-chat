package backend.xxx.chat.realtime.model;

import java.time.Instant;

import backend.xxx.chat.message.model.MessageStatus;

public record MessageDeliveredEventData(
        Long messageId,
        MessageStatus status,
        Instant deliveredAt
) {
}
