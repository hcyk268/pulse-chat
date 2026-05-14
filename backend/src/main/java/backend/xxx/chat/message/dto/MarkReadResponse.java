package backend.xxx.chat.message.dto;

import java.time.Instant;

public record MarkReadResponse(
        Long conversationId,
        Long lastReadMessageId,
        Instant readAt,
        long unreadCount
) {
}
