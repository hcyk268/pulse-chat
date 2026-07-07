package backend.xxx.chat.outbox.payload;

import java.time.Instant;

public record MessageDeliveredOutboxPayload(
        Long conversationId,
        Long receiverId,
        Long senderId,
        Long lastDeliveredMessageId,
        Instant deliveredAt
) {
}
