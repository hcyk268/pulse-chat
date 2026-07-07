package backend.xxx.chat.outbox.payload;

public record MessageCreatedOutboxPayload(
        Long conversationId,
        Long messageId
) {
}
