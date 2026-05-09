package backend.xxx.chat.user.dto;

public record UserSearchItemResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        PresenceResponse presence,
        Long directConversationId
) {
}
