package backend.xxx.chat.realtime.model;

import java.time.Instant;

public record MessageUnPinnedEventData(
        Long messageId,
        Instant unPinnedAt
) {
}
