package backend.xxx.chat.realtime.service;

import java.util.Collection;

import backend.xxx.chat.realtime.model.RealtimeEventType;

public interface RealtimeEventPublisher {

    default <T> void sendToUser(
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    ) {
        sendToUser(null, username, eventType, conversationId, data);
    }

    <T> void sendToUser(
            String eventId,
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    );

    default <T> void sendToUsers(
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
}
