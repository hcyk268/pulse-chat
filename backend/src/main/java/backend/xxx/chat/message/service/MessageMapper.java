package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.dto.MessageReplyResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        User sender = message.getSender();

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
                toReplyResponse(message.getReplyToMessage()),
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

    private MessageReplyResponse toReplyResponse(Message replyToMessage) {
        if (replyToMessage == null) {
            return null;
        }

        return new MessageReplyResponse(
                replyToMessage.getId(),
                toSummaryUserResponse(replyToMessage.getSender()),
                replyToMessage.isDeleted() ? null : replyToMessage.getContent(),
                replyToMessage.getMessageType(),
                replyToMessage.getCreatedAt(),
                replyToMessage.getDeletedAt()
        );
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
