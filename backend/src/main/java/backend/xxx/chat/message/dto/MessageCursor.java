package backend.xxx.chat.message.dto;

import java.time.Instant;

public record MessageCursor(
        Instant createdAt,
        Long messageId
) {
}
