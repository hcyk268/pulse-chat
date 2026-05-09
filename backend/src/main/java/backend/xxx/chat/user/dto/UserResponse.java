package backend.xxx.chat.user.dto;

import java.time.Instant;

import backend.xxx.chat.user.model.AccountStatus;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String bio,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
