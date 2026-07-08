package backend.xxx.chat.message.strategy;

import java.util.List;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.message.dto.AttachmentRequest;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.service.MessageAttachmentMapper;
import lombok.RequiredArgsConstructor;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaMessageStrategy implements MessageTypeStrategy {

    private final MessageAttachmentMapper messageAttachmentMapper;

    @Override
    public MessageType type() {
        return MessageType.MEDIA;
    }

    @Override
    public Message createMessage(Conversation conversation, User sender, SendMessageRequest request, Message replyToMessage) {
        List<AttachmentRequest> attachments = request.attachments();
        if (attachments == null || attachments.isEmpty()) {
            throw new ValidationException("Media message must contain at least one attachment");
        }

        Message message = Message.createMediaMessage(
                conversation,
                sender,
                request.clientMessageId(),
                request.content(),
                replyToMessage
        );

        for (int index = 0; index < attachments.size(); index++) {
            MessageAttachment attachment = messageAttachmentMapper.toEntity(attachments.get(index), index);
            message.addAttachment(attachment);
        }

        return message;
    }
}
