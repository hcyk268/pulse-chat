package backend.xxx.chat.outbox.dispatcher.impl;

import backend.xxx.chat.outbox.dispatcher.OutboxEventHandler;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.payload.MessageOutboxPayload;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.MessageRealtimeNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDeletedOutboxHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final MessageRealtimeNotifier notifier;

    @Override
    public boolean supports(String eventType) {
        return RealtimeEventType.MESSAGE_DELETED.getValue().equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            MessageOutboxPayload payload = objectMapper.readValue(event.getPayload(), MessageOutboxPayload.class);
            notifier.notifyDeleted(payload.conversationId(), payload.messageId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to handle message.deleted", ex);
        }
    }
}
