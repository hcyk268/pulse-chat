package backend.xxx.chat.realtime.service;

import java.time.Instant;
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
    private static final String BROADCAST_EVENTS_DESTINATION = "/topic/events";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public <T> void sendToUser(
            String eventId,
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        RealtimeEventEnvelope<T> envelope = createEnvelope(
                eventId,
                eventType,
                conversationId,
                data
        );

        messagingTemplate.convertAndSendToUser(
                username,
                USER_EVENTS_DESTINATION,
                envelope
        );
    }

    @Override
    public <T> void broadCast(String eventId, RealtimeEventType type, T data) {
        sendToTopic(BROADCAST_EVENTS_DESTINATION, eventId, type, data);
    }

    @Override
    public <T> void sendToTopic(
            String destination,
            String eventId,
            RealtimeEventType eventType,
            T data
    ) {
        RealtimeEventEnvelope<T> envelope = createEnvelope(
                eventId,
                eventType,
                null,
                data
        );

        messagingTemplate.convertAndSend(destination, envelope);
    }

    private <T> RealtimeEventEnvelope<T> createEnvelope(
            String eventId,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        return new RealtimeEventEnvelope<>(
                resolveEventId(eventId),
                eventType,
                Instant.now(),
                conversationId,
                data
        );
    }

    private String resolveEventId(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }

        return "evt_" + UUID.randomUUID();
    }
}
