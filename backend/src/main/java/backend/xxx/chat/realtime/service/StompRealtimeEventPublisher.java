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
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        RealtimeEventEnvelope<T> envelope = new RealtimeEventEnvelope<>(
                buildEventId(),
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

    @Override
    public <T> void sendToUsers(
            Collection<String> usernames,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        usernames.forEach(username -> sendToUser(
                username,
                eventType,
                conversationId,
                data
        ));
    }

    private String buildEventId() {
        return "evt_" + UUID.randomUUID();
    }
}
