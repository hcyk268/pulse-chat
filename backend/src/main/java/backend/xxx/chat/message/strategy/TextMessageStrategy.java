package backend.xxx.chat.message.strategy;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class TextMessageStrategy implements MessageTypeStrategy {

    @Override
    public MessageType type() {
        return MessageType.TEXT;
    }

    @Override
    public Message createMessage(
            Conversation conversation,
            User sender,
            SendMessageRequest request,
            Message replyToMessage
    ) {
        if (request.attachments() != null && !request.attachments().isEmpty()) {
            throw new ValidationException("Text message does not support attachments");
        }

        return Message.createTextMessage(
                conversation,
                sender,
                request.clientMessageId(),
                request.content(),
                replyToMessage
        );
    }
}
