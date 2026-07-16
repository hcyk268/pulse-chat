package backend.xxx.chat.conversation.dto;

import java.util.List;

import backend.xxx.chat.common.validation.InputValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateGroupConversationRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String avatarUrl,
        @NotNull @Size(min = 2, max = 100, message = "conversation.group.members.min")
        List<Long> memberIds
) {
}