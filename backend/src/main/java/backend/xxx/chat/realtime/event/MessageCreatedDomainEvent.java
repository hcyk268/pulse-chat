package backend.xxx.chat.realtime.event;

public record MessageCreatedDomainEvent(
        Long conversationId,
        Long messageId
) {
}