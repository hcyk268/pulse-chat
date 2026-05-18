package backend.xxx.chat.realtime.event;

import java.time.Instant;

public record MessageUnPinnedDomainEvent(
        Long conversationId,
        Long messageId,
        Instant unPinnedAt
) {
}
