package backend.xxx.chat.message.strategy;

import java.util.List;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.message.dto.AttachmentRequest;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.service.MessageAttachmentResolver;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaMessageStrategy implements MessageTypeStrategy {

    private final MessageAttachmentResolver messageAttachmentResolver;

    @Override
    public MessageType type() {
        return MessageType.MEDIA;
    }

    @Override
    public Message createMessage(Conversation conversation, User sender, SendMessageRequest request, Message replyToMessage) {
        Message message = Message.createMediaMessage(
                conversation,
                sender,
                request.clientMessageId(),
                request.content(),
                replyToMessage
        );

        List<MessageAttachment> attachments = messageAttachmentResolver.resolve(sender, request.attachments());
        attachments.forEach(message::addAttachment);

        return message;
    }
}