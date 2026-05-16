package backend.xxx.chat.realtime.model;


import java.time.Instant;

public record MessageReadEventData(
        Long readerId,
        String readerUsername,
        Long lastReadMessageId,
        Instant readAt
) {
}
