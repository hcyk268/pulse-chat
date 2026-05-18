package backend.xxx.chat.message.dto;

import backend.xxx.chat.message.model.MessageReactionEmoji;
import jakarta.validation.constraints.NotNull;

public record MessageReactionRequest(
        @NotNull MessageReactionEmoji emoji
) {
}
