package backend.xxx.chat.auth.model;


public record RefreshTokenSession(
        Long userId,
        String username,
        String sessionId
) {
}
