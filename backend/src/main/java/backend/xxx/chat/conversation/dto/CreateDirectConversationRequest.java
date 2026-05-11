package backend.xxx.chat.conversation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDirectConversationRequest(
        @NotNull @Positive Long targetUserId
) {
}
