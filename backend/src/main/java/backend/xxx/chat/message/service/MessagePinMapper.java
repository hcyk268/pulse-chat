package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.UnPinMessageResponse;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MessagePinMapper {

    private final MessageMapper messageMapper;

    public MessagePinResponse toResponse(MessagePin messagePin) {
        User pinnedBy = messagePin.getPinnedBy();

        return new MessagePinResponse(
                messagePin.getId(),
                messageMapper.toResponse(messagePin.getMessage()),
                new SummarizeUserResponse(
                        pinnedBy.getId(),
                        pinnedBy.getUsername(),
                        pinnedBy.getDisplayName(),
                        pinnedBy.getAvatarUrl()
                ),
                messagePin.getPinnedAt()
        );
    }

    public UnPinMessageResponse toUnPinMessageResponse(Long conversationId, Long messageId, Instant unpinnedAt) {
        return new UnPinMessageResponse(conversationId, messageId, unpinnedAt);
    }
}
