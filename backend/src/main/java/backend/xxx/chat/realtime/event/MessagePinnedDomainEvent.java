package backend.xxx.chat.realtime.event;

public record MessagePinnedDomainEvent(
        Long conversationId,
        Long messagePinId
) {
}
