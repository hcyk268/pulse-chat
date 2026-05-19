package backend.xxx.chat.message.strategy;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.model.User;

public interface MessageTypeStrategy {

    MessageType type();

    Message createMessage(
            Conversation conversation,
            User sender,
            SendMessageRequest request,
            Message replyToMessage
    );
}
