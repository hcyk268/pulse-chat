package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.MessageResponse;
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
                message.getContent(),
                message.getMessageType(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }
}
