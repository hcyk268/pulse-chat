package backend.xxx.chat.user.service;

import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.dto.UserResponse;
import backend.xxx.chat.user.dto.UserSearchItemResponse;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserSearchItemResponse toSearchItem(User user, Presence presence, Long directConversationId) {
        return new UserSearchItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                toPresenceResponse(presence),
                directConversationId
        );
    }

    private PresenceResponse toPresenceResponse(Presence presence) {
        return new PresenceResponse(
                presence != null && presence.isOnline(),
                presence == null ? null : presence.getLastActiveAt()
        );
    }
}
