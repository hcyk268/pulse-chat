package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.*;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessageRead;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final MessageAttachmentMapper messageAttachmentMapper;

    public MessageResponse toResponse(Message message, Map<Long, List<MessageAttachment>> attachmentsByMessageId) {
        User sender = message.getSender();
        Message replyToMessage = message.getReplyToMessage();

        return new MessageResponse(
                message.getId(),
                message.getClientMessageId().toString(),
                message.getConversation().getId(),
                new SummarizeUserResponse(
                        sender.getId(),
                        sender.getUsername(),
                        sender.getDisplayName(),
                        sender.getAvatarUrl()
                ),
                message.isDeleted() ? null : message.getContent(),
                toReplyResponse(replyToMessage, attachmentsFor(replyToMessage, attachmentsByMessageId)),
                messageAttachmentMapper.toResponses(attachmentsFor(message, attachmentsByMessageId)),
                message.getMessageType(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedBy() == null ? null : toSummaryUserResponse(message.getDeletedBy()),
                message.getDeletedAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }


    public MessageReadReceiptsResponse toReadReceiptsResponse(Long messageId, java.util.List<MessageRead> reads) {
        return new MessageReadReceiptsResponse(
                messageId,
                reads.stream()
                        .map(this::toReadReceiptResponse)
                        .toList()
        );
    }

    private MessageReadReceiptResponse toReadReceiptResponse(MessageRead read) {
        return new MessageReadReceiptResponse(
                toSummaryUserResponse(read.getUser()),
                read.getReadAt()
        );
    }

    private MessageReplyResponse toReplyResponse(Message replyToMessage, List<MessageAttachment> messageAttachments) {
        if (replyToMessage == null) {
            return null;
        }

        return new MessageReplyResponse(
                replyToMessage.getId(),
                toSummaryUserResponse(replyToMessage.getSender()),
                replyToMessage.isDeleted() ? null : replyToMessage.getContent(),
                replyToMessage.isDeleted() ? List.of() : messageAttachmentMapper.toResponses(messageAttachments),
                replyToMessage.getMessageType(),
                replyToMessage.getCreatedAt(),
                replyToMessage.getDeletedAt()
        );
    }

    private List<MessageAttachment> attachmentsFor(
            Message message,
            Map<Long, List<MessageAttachment>> attachmentsByMessageId
    ) {
        if (message == null || message.isDeleted()) {
            return List.of();
        }

        return attachmentsByMessageId.getOrDefault(message.getId(), List.of());
    }

    private SummarizeUserResponse toSummaryUserResponse(User user) {
        return new SummarizeUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
