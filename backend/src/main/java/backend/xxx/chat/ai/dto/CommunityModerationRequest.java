package backend.xxx.chat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityModerationRequest(
        @Size(max = 160) String title,
        @NotBlank @Size(max = 8_000) String content,
        @Size(max = 120) String communitySlug
) {
}