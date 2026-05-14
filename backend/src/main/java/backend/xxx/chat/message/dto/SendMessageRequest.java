package backend.xxx.chat.message.dto;

import backend.xxx.chat.message.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull Long conversationId,
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 4000) String content,
        @NotNull MessageType messageType
) {
}
