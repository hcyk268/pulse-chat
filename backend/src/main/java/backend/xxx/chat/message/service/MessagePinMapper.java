package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class MessagePinMapper {

    public MessagePinResponse toResponse(MessagePin messagePin) {
        User pinnedBy = messagePin.getPinnedBy();

        return new MessagePinResponse(
                messagePin.getId(),
                messagePin.getConversation().getId(),
                messagePin.getMessage().getId(),
                new SummarizeUserResponse(
                        pinnedBy.getId(),
                        pinnedBy.getUsername(),
                        pinnedBy.getDisplayName(),
                        pinnedBy.getAvatarUrl()
                ),
                messagePin.getPinnedAt()
        );
    }
}
