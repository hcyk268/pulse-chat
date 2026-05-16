package backend.xxx.chat.realtime.event;

import java.time.Instant;

public record PresenceUpdatedDomainEvent(
        Long userId,
        String username,
        boolean online,
        Instant lastActiveAt
) {
}
