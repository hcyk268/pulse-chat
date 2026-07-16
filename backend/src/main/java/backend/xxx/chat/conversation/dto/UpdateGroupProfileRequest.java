package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGroupProfileRequest(
        @Size(max = 100)
        @Pattern(regexp = InputValidationPatterns.NO_HTML_ANGLE_BRACKETS, message = "conversation.group.name.no-html")
        String name,

        @Size(max = 500)
        @Pattern(regexp = InputValidationPatterns.OPTIONAL_HTTP_URL, message = "user.avatar-url.invalid")
        String avatarUrl
) {
}
