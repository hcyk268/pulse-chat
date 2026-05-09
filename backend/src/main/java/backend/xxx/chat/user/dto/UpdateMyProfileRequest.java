package backend.xxx.chat.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String bio
) {
}
