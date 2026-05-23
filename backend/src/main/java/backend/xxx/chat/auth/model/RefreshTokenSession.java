package backend.xxx.chat.auth.model;

import java.time.Instant;

public record RefreshTokenSession(
        Long userId,
        String username,
        String sessionId
) {
}
