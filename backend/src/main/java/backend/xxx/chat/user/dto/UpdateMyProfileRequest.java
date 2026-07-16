package backend.xxx.chat.user.dto;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 100)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "user.display-name.no-html")
        String displayName,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.OPTIONAL_HTTP_URL, message = "user.avatar-url.invalid")
        String avatarUrl,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "user.bio.no-html")
        String bio
) {
}
