package backend.xxx.chat.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {
}
