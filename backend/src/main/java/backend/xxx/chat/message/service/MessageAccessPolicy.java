package backend.xxx.chat.message.service;

import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class MessageAccessPolicy {

    public void requireSender(Message message, User user, String messageText) {
        if (!isSender(message, user)) {
            throw new ForbiddenException(messageText);
        }
    }

    public void requireNotSender(Message message, User user, String messageText) {
        if (isSender(message, user)) {
            throw new ForbiddenException(messageText);
        }
    }

    private boolean isSender(Message message, User user) {
        if (message == null || message.getSender() == null || message.getSender().getId() == null) {
            throw new ValidationException("message sender must not be null");
        }

        if (user == null || user.getId() == null) {
            throw new ValidationException("userId must not be null");
        }

        return message.getSender().getId().equals(user.getId());
    }
}
