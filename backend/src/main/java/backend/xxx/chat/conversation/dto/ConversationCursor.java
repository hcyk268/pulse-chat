package backend.xxx.chat.conversation.dto;

import java.time.Instant;

public record ConversationCursor(
        Instant cursorAt,
        Long conversationId
) {
}
