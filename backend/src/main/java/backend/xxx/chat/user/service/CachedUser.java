package backend.xxx.chat.user.service;

import java.time.Instant;

import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.User;

public record CachedUser(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String bio,
        AccountStatus accountStatus,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {

    public static CachedUser from(User user) {
        return new CachedUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
