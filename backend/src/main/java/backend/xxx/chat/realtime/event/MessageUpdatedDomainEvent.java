package backend.xxx.chat.realtime.event;

public record MessageUpdatedDomainEvent(
        Long conversationId,
        Long messageId
) {
}
