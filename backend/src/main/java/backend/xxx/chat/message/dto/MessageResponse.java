package backend.xxx.chat.message.dto;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessageResponse(
        Long id,
        String clientMessageId,
        Long conversationId,
        SummarizeUserResponse sender,
        String content,
        MessageReplyResponse replyTo,
        List<AttachmentResponse> attachments,
        MessageType messageType,
        MessageStatus status,
        Instant createdAt,
        Instant editedAt,
        SummarizeUserResponse deletedBy,
        Instant deletedAt,
        Instant deliveredAt,
        Instant readAt
) {
}
