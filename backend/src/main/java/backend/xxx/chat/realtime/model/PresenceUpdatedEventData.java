package backend.xxx.chat.realtime.model;

import java.time.Instant;

public record PresenceUpdatedEventData(
        Long userId,
        String username,
        boolean isOnline,
        Instant lastActiveAt
) {
}
