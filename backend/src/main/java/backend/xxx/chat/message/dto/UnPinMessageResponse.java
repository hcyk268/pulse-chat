package backend.xxx.chat.message.dto;

import java.time.Instant;

public record UnPinMessageResponse(
        Long conversationId,
        Long messageId,
        Instant unpinnedAt
) {
}