package backend.xxx.chat.realtime.event;

import java.time.Instant;

public record MessageDeliveredDomainEvent(
        Long conversationId,
        Long receiverId,
        Long senderId,
        Long lastDeliveredMessageId,
        Instant deliveredAt
) {
}
