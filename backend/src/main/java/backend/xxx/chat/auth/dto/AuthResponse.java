package backend.xxx.chat.auth.dto;

import backend.xxx.chat.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInMs,
        long refreshTokenExpiresInMs,
        UserResponse user
) {
}
