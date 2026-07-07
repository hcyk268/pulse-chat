package backend.xxx.chat.auth.dto;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = InputValidationPatterns.USERNAME, message = "username may only contain letters, numbers, dot, underscore, or hyphen")
        String username,

        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "displayName must not contain HTML tags")
        String displayName,

        @NotBlank @Size(min = 8, max = 100) String password,

        @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {
}
