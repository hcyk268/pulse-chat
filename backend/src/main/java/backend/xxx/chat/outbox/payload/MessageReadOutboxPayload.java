package backend.xxx.chat.outbox.payload;

import java.time.Instant;

public record MessageReadOutboxPayload(
        Long conversationId,
        Long readerId,
        Long lastReadMessageId,
        Instant readAt
) {
}
