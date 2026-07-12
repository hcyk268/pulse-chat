package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGroupProfileRequest(
        @Size(max = 100)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "name must not contain HTML tags")
        String name,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.OPTIONAL_HTTP_URL, message = "avatarUrl must start with http:// or https://")
        String avatarUrl
) {
}
