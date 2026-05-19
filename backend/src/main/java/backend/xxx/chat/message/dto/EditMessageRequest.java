package backend.xxx.chat.message.dto;

import backend.xxx.chat.message.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
        @NotBlank @Size(max = 4000)
        String newContent,
        MessageType type
) {
}
