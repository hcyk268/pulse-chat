package backend.xxx.chat.realtime.event;

public record MessageDeletedDomainEvent(
        Long conversationId,
        Long messageId
) {
}
