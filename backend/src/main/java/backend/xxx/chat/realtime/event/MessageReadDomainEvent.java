package backend.xxx.chat.realtime.event;

import java.time.Instant;

public record MessageReadDomainEvent(
        Long conversationId,
        Long readerId,
        Long lastReadMessageId,
        Instant readAt
) {
}