package backend.xxx.chat.realtime.dto;

import java.time.Instant;

import backend.xxx.chat.realtime.model.RealtimeEventType;

public record RealtimeEventEnvelope<T>(
        String eventId,
        RealtimeEventType eventType,
        Instant occurredAt,
        Long conversationId,
        T data
) {
}
