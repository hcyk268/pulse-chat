package backend.xxx.chat.user.dto;

public record SummarizeUserResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {
}
