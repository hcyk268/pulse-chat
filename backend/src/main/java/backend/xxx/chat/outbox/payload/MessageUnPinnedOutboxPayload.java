package backend.xxx.chat.outbox.payload;

import java.time.Instant;

public record MessageUnPinnedOutboxPayload(
        Long conversationId,
        Long messageId,
        Instant unPinnedAt
) {
}
