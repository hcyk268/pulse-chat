package backend.xxx.chat.user.dto;

import java.time.Instant;

public record PresenceResponse(
        boolean isOnline,
        Instant lastActiveAt
) {
}
