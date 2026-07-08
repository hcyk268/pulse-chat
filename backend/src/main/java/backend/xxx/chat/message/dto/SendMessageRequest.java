package backend.xxx.chat.message.dto;

import backend.xxx.chat.message.model.MessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull Long conversationId,
        @NotNull UUID clientMessageId,
        @Size(max = 4000) String content,
        @NotNull MessageType messageType,
        Long replyToMessageId,
        List<@Valid AttachmentRequest> attachments
) {
}
