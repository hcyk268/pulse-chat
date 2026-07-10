package backend.xxx.chat.realtime.service;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import backend.xxx.chat.realtime.dto.RealtimeEventEnvelope;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompRealtimeEventPublisher implements RealtimeEventPublisher {

    private static final String USER_EVENTS_DESTINATION = "/queue/events";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public <T> void sendToUser(
            String eventId,
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        RealtimeEventEnvelope<T> envelope = new RealtimeEventEnvelope<>(
                resolveEventId(eventId),
                eventType,
                Instant.now(),
                conversationId,
                data
        );

        messagingTemplate.convertAndSendToUser(
                username,
                USER_EVENTS_DESTINATION,
                envelope
        );
    }

    private String resolveEventId(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }

        return "evt_" + UUID.randomUUID();
    }
}
