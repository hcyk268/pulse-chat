package backend.xxx.chat.message.service;

import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.UnPinMessageResponse;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessagePinMapper {

    private final MessageMapper messageMapper;

    public MessagePinResponse toResponse(
            MessagePin messagePin,
            Map<Long, List<MessageAttachment>> attachmentsByMessageId
    ) {
        User pinnedBy = messagePin.getPinnedBy();

        return new MessagePinResponse(
                messagePin.getId(),
                messageMapper.toResponse(messagePin.getMessage(), attachmentsByMessageId),
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
