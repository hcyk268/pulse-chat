package backend.xxx.chat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Size(max = 512)
        String token,

        @NotBlank
        @Size(min = 12, max = 72)
        String newPassword,

        @NotBlank
        @Size(min = 12, max = 72)
        String confirmPassword
) {
}

