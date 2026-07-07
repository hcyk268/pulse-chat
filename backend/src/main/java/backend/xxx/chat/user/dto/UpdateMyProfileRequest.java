package backend.xxx.chat.user.dto;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 100)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "displayName must not contain HTML tags")
        String displayName,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.OPTIONAL_HTTP_URL, message = "avatarUrl must start with http:// or https://")
        String avatarUrl,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "bio must not contain HTML tags")
        String bio
) {
}
