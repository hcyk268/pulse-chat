package backend.xxx.chat.realtime.service;

import java.util.Collection;

import backend.xxx.chat.realtime.model.RealtimeEventType;

public interface RealtimeEventPublisher {

    <T> void sendToUser(
            String username,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    );

    <T> void sendToUsers(
            Collection<String> usernames,
            RealtimeEventType eventType,
            Long conversationId,
            T data
    );
}
