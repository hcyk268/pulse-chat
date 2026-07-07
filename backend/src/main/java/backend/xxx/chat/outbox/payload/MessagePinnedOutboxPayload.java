package backend.xxx.chat.outbox.payload;

public record MessagePinnedOutboxPayload(
        Long conversationId,
        Long messagePinId
) {
}
