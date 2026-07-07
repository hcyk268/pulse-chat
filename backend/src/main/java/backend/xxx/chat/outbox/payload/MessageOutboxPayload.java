package backend.xxx.chat.outbox.payload;

public record MessageOutboxPayload(
        Long conversationId,
        Long messageId
) {
}
